SmoothSimpleButton :SmoothButton {

    mouseUp {arg x, y, modifiers;
        if( pressed == true ) // pressed can never be true if not enabled
            { mouseUpAction.value(this, x, y, modifiers);
            pressed = false;
            this.doAction;this.refresh;
        };
    }

}