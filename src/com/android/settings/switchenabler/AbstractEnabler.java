package com.android.settings.switchenabler;

import android.widget.Switch;

public abstract class AbstractEnabler {

    public abstract void setSwitch(Switch switch_);
    public void start() {}; //subclass could not override this method if not use Observer.
    public abstract void resume();
    public abstract void pause();
    public void stop() {}; //subclass could not override this method if not use Observer.

}

