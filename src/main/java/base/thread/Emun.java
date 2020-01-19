package base.thread;

public enum Emun {
	SUCCESS(200, "成功"), FAILED(500, "失败");
	int code;
	String message;
	Emun(int code, String message) {
		this.code=code;
		this.message=message;
	}
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	static class Demo{
		public static void main(String[] args) {
			SUCCESS.setCode(300);
			System.out.println(SUCCESS.getCode());
			
		}
	}
}
