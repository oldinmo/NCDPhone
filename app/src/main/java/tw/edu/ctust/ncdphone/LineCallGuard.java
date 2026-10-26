package tw.edu.ctust.ncdphone;

/** Uncertain call UI never implies an ended call, regardless of elapsed time. */
final class LineCallGuard {
    boolean active,sawCall;int endedChecks;
    void start(){active=true;sawCall=false;endedChecks=0;}
    void clear(){active=false;sawCall=false;endedChecks=0;}
    boolean observe(boolean hangupVisible,boolean audioActive,boolean verifiedChat){
        if(!active)return false;
        if(hangupVisible||audioActive){sawCall=true;endedChecks=0;return false;}
        if(!verifiedChat||!sawCall){endedChecks=0;return false;}
        if(++endedChecks<3)return false;
        clear();return true;
    }
}
