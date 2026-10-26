package tw.edu.ctust.ncdphone;

/** Face-down dwell followed by lifting upright or turning face-up. No wall-clock dependency. */
final class FlipDetector {
    private long downSince=-1;
    void reset(){downSince=-1;}
    boolean update(float z,long now){
        if(z < -6.5f){if(downSince<0)downSince=now;return false;}
        if(z > -1f && downSince>=0){boolean flipped=now-downSince>=600;reset();return flipped;}
        return false;
    }
}
