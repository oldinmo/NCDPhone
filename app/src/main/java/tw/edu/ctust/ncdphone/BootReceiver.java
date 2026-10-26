package tw.edu.ctust.ncdphone;
import android.content.*;
import android.app.role.RoleManager;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())&&Store.flag(c,"bootHome")&&c.getSystemService(RoleManager.class).isRoleHeld(RoleManager.ROLE_HOME)){try{Ui.home(c);}catch(Exception ignored){/* OS launches the selected HOME after unlock. */}}}
}
