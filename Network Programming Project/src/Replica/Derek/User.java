package store;


public class User {
	
	protected String userId;
	protected String region;
	
	protected User (String _userId, String _region) {
		setUserId(_userId);
		setRegion(_region);
	}
	
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}
}
