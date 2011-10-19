UAbstractWindow {

    classvar <currentDict, <allDict;
    var <window, <view;

    *initClass {
        currentDict = ();
        allDict = ();
    }
    *current { ^currentDict[this] }

    *current_ { |x| currentDict[this] = x }

    toFront {
        if( window.isClosed.not ) {
         window.front;
        };
    }

    addToAll {
        allDict[this.class] = allDict[this.class].asCollection.add( this );
    }

    removeFromAll { if( allDict[this.class].notNil ) { allDict[this.class].remove( this ); }; }

    newWindow { |bounds, title, onClose, background, margin, gap|

		var font = Font( Font.defaultSansFace, 11 );
        bounds = bounds ? Rect(230 + 20.rand2, 230 + 20.rand2, 680, 300);

        window = Window(title, bounds).front;
        window.onClose_(onClose);
        //for 3.5 this has to be changed.
        if(window.respondsTo(\drawFunc_)) {
            window.drawFunc_({ currentDict[this.class] = this });
        } {
            window.drawHook_({ currentDict[this.class] = this });
        };
        margin = margin ? 4;
        gap = gap ? 2;
        view = window.view;
        view.background_( background ? Color.grey(0.5) );
        view.resize_(5);
    }


}