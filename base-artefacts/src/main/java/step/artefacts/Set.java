package step.artefacts;

import step.artefacts.handlers.SetHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(handler = SetHandler.class, block=false)
public class Set extends AbstractArtefact {

	@DynamicAttribute
	private String key;
	
	@DynamicAttribute
	private String value;
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
