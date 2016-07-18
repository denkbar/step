package step.commons.pools.selectionpool;

public class WaitingPretender<P extends Identity, F extends Identity> {

	final P pretender;

	Token<F> associatedToken;
	
	public WaitingPretender(P pretender) {
		super();
		this.pretender = pretender;
	}
}
