package logger;

public class Logger {

	public enum Level { ALL, DEBUG, INFO, WARN, ERROR, FATAL, OFF };
	private int level = 0;

	public void setLevel(Level level) {
		switch (level) {
		case ALL:
			this.level = 0;
			break;
		case DEBUG:
			this.level = 1;
			break;
		case INFO:
			this.level = 2;
			break;
		case WARN:
			this.level = 3;
			break;
		case ERROR:
			this.level = 4;
			break;
		case FATAL:
			this.level = 5;
			break;
		default:
			this.level = 6;
		}
	}

	public void debug(String message) {
		if (level <= 1) {
			System.out.println(message);
		}
	}

	public void info(String message) {
		if (level <= 2) {
			System.out.println("[INFO] " + message);
		}
	}

	public void warn(String message) {
		if (level <= 3) {
			System.out.println("[WARN] " + message);
		}
	}

	public void error(String message) {
		if (level <= 4) {
			System.err.println("[ERROR] " + message);
		}
	}

	public void fatal(String message) {
		if (level <= 5) {
			System.err.println("[FATAL] " + message);
		}
	}
}
