/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *  
 * This file is part of rtm
 *  
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.plugins.events;

import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * @author doriancransac
 *
 */
public class EventBroker {

	private ConcurrentHashMap<String, Event> events;
	private long circuitBreakerThreshold;
	private boolean advancedStatsOn;

	private LongAdder cumulatedPuts;
	private LongAdder cumulatedGets;
	private LongAdder cumulatedAttemptedGets;
	private LongAdder cumulatedAttemptedGroupGets;
	private LongAdder cumulatedPeeks;

	private int sizeWaterMark = 0;

	public static String DEFAULT_GROUP_VALUE = "<default>";

	public EventBroker(){
		this.circuitBreakerThreshold = 5000L;
		this.advancedStatsOn = true;
		init();
	}

	public EventBroker(long circuitBreakerThreshold, boolean advancedStatsOn){
		this.circuitBreakerThreshold = circuitBreakerThreshold;
		this.advancedStatsOn = advancedStatsOn;
		init();
	}

	private void init(){
		events = new ConcurrentHashMap<String, Event>();
		initStats();
	}

	public void clear(){
		events.clear();
	}

	public String toString(){
		return events.toString();
	}

	public Map<String, Event> getIdBasedEventMap() {
		return events;
	}

	public Map<String, Event> getIdBasedEventMap(int skip, int limit) {
		return events.values().stream()
				.skip(skip)
				.limit(limit)
				.collect(Collectors.toMap(Event::getId, e -> e));
	}

	public long getCircuitBreakerThreshold() {
		return circuitBreakerThreshold;
	}

	public void setCircuitBreakerThreshold(long circuitBreakerThreshold) {
		this.circuitBreakerThreshold = circuitBreakerThreshold;
	}

	public int getSize(){
		return events.size();
	}

	/* Privatized to force atomic use of "has+get+remove" in one concept (get) by client */
	private boolean hasEvent(String id){
		if(id == null)
			return false;
		else
			return events.containsKey(id);
	}

	private boolean hasEvent(String group, String name){
		return hasEvent(lookup(group, name));
	}
	/**/

	/** Main primitives, based on id **/

	public Event put(Event event) throws Exception{
		if(event == null)
			throw new Exception("Event is null.");

		if(event.getGroup() == null)
			event.setGroup(DEFAULT_GROUP_VALUE);

		Event ret = null;
		Event putRetEvent = null;
		String mapKey = null;

		int size = events.size();

		if(size >= this.circuitBreakerThreshold)
			throw new Exception("Broker size exceeds " + this.circuitBreakerThreshold + " events. Circuit breaker is on.");

		if(event.getId() == null || event.getId().isEmpty()){
			mapKey = UUID.randomUUID().toString();
			event.setId(mapKey);

			//we're in the Group use case, so we prefer to return the event itself (benefit: returning the uuid to the user) 
			ret = event;

		}else{
			mapKey = event.getId();
		} 

		event.setInsertionTimestamp(System.currentTimeMillis());

		// we want to return the previous value in the Id use case (putRetEvent)
		putRetEvent = events.put(mapKey, event);

		if(this.advancedStatsOn){
			this.cumulatedPuts.increment();

			// we're avoiding to call CHM.size() again which is an expensive call
			if(size + 1 > this.sizeWaterMark){ 
				this.sizeWaterMark = size + 1;
			}
		}

		return ret==null?putRetEvent:ret;
	}

	public Event get(String id){
		if(this.advancedStatsOn){
			this.cumulatedAttemptedGets.increment();
		}
		if(id == null || id.isEmpty())
			return null;
		Event ret = events.remove(id);
		if(this.advancedStatsOn){
			if(ret != null) //we only count "real" gets (which found and returned an event)
				this.cumulatedGets.increment();
		}
		if(ret != null)
			ret.setDeletionTimestamp(System.currentTimeMillis());
		return ret;
	}

	public Event peek(String id){
		if(id == null || id.isEmpty())
			return null;
		Event ret = events.get(id);
		if(this.advancedStatsOn){
			if(ret != null) //we only count "real" peeks (which found and returned an event)
				this.cumulatedPeeks.increment();
		}
		if(ret != null)
			ret.setLastReadTimestamp(System.currentTimeMillis());
		return ret;
	}
	/****/

	/** Lookup (used as an adapter from Group use case to Id use case **/

	private String lookup(String searchedGroup, String searchedName){
		if(searchedGroup == null || searchedGroup.isEmpty() || searchedGroup.equals("null"))
			throw new RuntimeException("group can not be null, empty or \"null\", found value=" + searchedGroup);

		Optional<Event> event;

		// loose group search
		if(searchedGroup.equals("*") || searchedName == null || searchedName.isEmpty() || searchedName.equals("null")){
			event = lookupLooseGroupBasedEvent(searchedGroup);
		}
		else{ // narrow name-based search
			event = lookupNamedGroupBasedEvent(searchedGroup, searchedName);
		}

		if(event.isPresent())
			return event.get().getId();
		else
			return null;
	}

	/** Here we're trying to protect against collisions, i.e concurrent removals of same-id events**/
	private Event lookupForRemove(String searchedGroup, String searchedName){
		String hit = lookup(searchedGroup, searchedName);
		Event ret = null;
		while(
				hit != null // if hit is null, either there was never a match or another thread stole the last key
				&& (ret = events.remove(hit)) == null){ // someone just stole the last key
			hit = lookup(searchedGroup, searchedName); // let's try our luck again until we successfully remove a key or run out of matching keys (null)
		}

		if(this.advancedStatsOn){
			if(ret != null) //we only count "real" gets (which found and returned an event)
				this.cumulatedGets.increment();
		}
		return ret;
	}


	private Optional<Event> lookupNamedGroupBasedEvent(String searchedGroup, String searchedName){
		return events.values().stream()
				.filter(e -> e.getGroup().equals(searchedGroup) || searchedGroup.equals("*"))
				.filter(e -> e.getName().equals(searchedName))
				.findAny();
	}

	private Optional<Event> lookupLooseGroupBasedEvent(String searchedGroup){
		return events.values().stream()
				.filter(e -> e.getGroup().equals(searchedGroup) || searchedGroup.equals("*"))
				.findAny();
	}

	/** Group primitives, adapted via lookup() to Id primitives **/

	public Event peek(String group, String name){
		return peek(lookup(group,name));
	}

	public void clearGroup(String group) {
		while(hasEvent(group, null))
			events.remove(lookup(group, null));
	}

	/** Now using a sync-free optimistic version of the group lookup as an alternative to syncGroupOn **/
	public Event get(String group, String name){
		if(this.advancedStatsOn){
			this.cumulatedAttemptedGroupGets.increment();
		}
		return lookupForRemove(group, name);
	}

	/****/

	/** Stats **/

	public void setAdvancedStatsOn(boolean statsOn) {
		this.advancedStatsOn = statsOn;
	}

	public boolean getAdvancedStatsOn() {
		return this.advancedStatsOn;
	}

	public long getCumulatedPuts() {
		return cumulatedPuts.longValue();
	}

	public long getCumulatedGets() {
		return cumulatedGets.longValue();
	}

	public long getCumulatedAttemptedGets() {
		return cumulatedAttemptedGets.longValue();
	}

	public long getCumulatedAttemptedGroupGets() {
		return cumulatedAttemptedGroupGets.longValue();
	}

	public long getCumulatedPeeks() {
		return cumulatedPeeks.longValue();
	}

	/** Unreliable due to the nature of CHM **/
	public int getSizeWaterMark() {
		return sizeWaterMark;
	}

	public Event findOldestEvent(){
		Optional<Event> event = events.values().stream().min(Comparator.comparing(Event::getInsertionTimestamp));
		if(event.isPresent())
			return event.get();
		else
			return null;
	}

	public Event findYoungestEvent(){
		Optional<Event> event = events.values().stream().max(Comparator.comparing(Event::getInsertionTimestamp));
		if(event.isPresent())
			return event.get();
		else
			return null;
	}

	public int getSizeForGroup(String group) {
		try{
			return events.values().stream()
					.filter(e -> e.getGroup().equals(group))
					.map(e -> 1)
					.reduce((x,y) -> x+y).get();
		}catch(NoSuchElementException e){
			return 0;
		}

	}

	public Event findYoungestEventForGroup(String group) {
		return events.values().stream().filter(e -> e.getGroup().equals(group)).max(Comparator.comparing(Event::getInsertionTimestamp)).get();
	}

	public Event findOldestEventForGroup(String group) {
		return events.values().stream().filter(e -> e.getGroup().equals(group)).min(Comparator.comparing(Event::getInsertionTimestamp)).get();
	}

	public void clearStats() {
		initStats();
	}

	private void initStats(){
		this.cumulatedPuts = new LongAdder();
		this.cumulatedGets = new LongAdder();
		this.cumulatedAttemptedGets = new LongAdder();
		this.cumulatedAttemptedGroupGets = new LongAdder();
		this.cumulatedPeeks = new LongAdder();
	}
	/****/

	/** Group access **/

	public Set<Event> getGroupEvents(String group){
		return getGroupEvents(group, 0, Integer.MAX_VALUE);
	}

	public Set<Event> getGroupEvents(String group, int skip, int limit){
		return events.values().stream()
				.filter(e -> e.getGroup().equals(group))
				.skip(skip)
				.limit(limit)
				.collect(Collectors.toSet());
	}

	public Map<String, Set<Event>> getFullGroupBasedEventMap(){
		return getGroupBasedEventMap(0, Integer.MAX_VALUE);
	}

	public Map<String, Set<Event>> getGroupBasedEventMap(int skip, int limit){
		return events.values().stream()
				.skip(skip)
				.limit(limit)
				.collect(Collectors.groupingBy(Event::getGroup, Collectors.toSet()));
	}

	public Set<String> getDistinctGroupNames(){
		return events.values().stream()
				.map(e -> e.getGroup())
				.distinct()
				.collect(Collectors.toSet());
	}
	/****/
}

