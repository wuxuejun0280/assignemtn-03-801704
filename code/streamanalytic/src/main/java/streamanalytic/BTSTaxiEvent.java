package streamanalytic;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BTSTaxiEvent {
	public String venderID;
	public String doLocation;
	public String puLocation;
	public Date pickupTime;
	public Date dropoffTime;

	public String toString() {
		SimpleDateFormat smf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return venderID+","+doLocation+","+puLocation+","+smf.format(pickupTime)+","+smf.format(dropoffTime);
	}

}
