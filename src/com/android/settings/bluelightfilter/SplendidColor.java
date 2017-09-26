package com.android.settings.bluelightfilter;

public class SplendidColor {

    public double h,s,v;

    public SplendidColor(double[] hsv){
        init(hsv);
    }

    public SplendidColor(double h, double s, double v){
        this.h = h;
        this.s = s;
        this.v = v;
    }

    private void init(double[] hsv){
        this.h = hsv[0];
        this.s = hsv[1];
        this.v = hsv[2];
    }

    @Override
    public boolean equals(Object o) {
        if(super.equals(o)){
            return true;
        }

        if(o instanceof SplendidColor){
            SplendidColor otherColor = (SplendidColor)o;
            return  otherColor.h == this.h &&
                    otherColor.s == this.s &&
                    otherColor.v == this.v;
        }
        return false;
    }

    public boolean equalsS(Object o) {
        if(o instanceof SplendidColor){
            SplendidColor otherColor = (SplendidColor)o;
            return  otherColor.s == this.s;
        }
        return false;
    }

    public boolean equalsH(Object o) {
        if(o instanceof SplendidColor){
            SplendidColor otherColor = (SplendidColor)o;
            return  otherColor.h == this.h;
        }
        return false;
    }

    @Override
    public SplendidColor clone(){
        return new SplendidColor(this.h, this.s, this.v);
    }

    public double[] getHSV(){
        return  new double[]{this.h,this.s,this.v};
    }

    @Override
    public String toString() {
        return "h " + h + " s " + s + " v " + v;
    }
}