UServerCenter{
    classvar <>servers;

    *initClass {
        servers = [Server.default]
    }
}

UScoreEditorGUI {
	
	classvar <>current, <all;

	var <scoreEditor;

	var <>scoreView, <>window, <tranportBar, topBar;
	var <usessionMouseEventsManager;

	//*initClass { UI.registerForShutdown({ scoreEditor.askForSave = false }); }

	*new { |scoreEditor, bounds|
		^super.new.init( scoreEditor)
			.addToAll
			.newWindow(bounds)
	}

	*currentSelectedEvents{
	    ^current.selectedEvents
	}

    init { |inScoreEditor|
        scoreEditor = if(inScoreEditor.class == UScore) {
            UScoreEditor(inScoreEditor)
        } {
            inScoreEditor;
        };
    }

	toFront {
	    if( window.isClosed.not ) {
	     window.front;
	    };
	}

	addToAll {
		all = all.asCollection.add( this );
	}

	removeFromAll { if( all.notNil ) { all.remove( this ); }; }

	score { ^scoreEditor.score }
	editor { ^scoreEditor }
	currentScore { ^scoreEditor.currentScore }
	currentEditor { ^scoreEditor.currentEditor }
	selectedEvents{ ^scoreView.selectedEvents }


	newWindow { |bounds|

		var font = Font( Font.defaultSansFace, 11 ), header, windowTitle, margin, gap, topBarH, tranBarH, view, centerView, centerBounds;
        bounds = bounds ? Rect(230 + 20.rand2, 230 + 20.rand2, 680, 300);

        window = Window("Score Editor", bounds).front;
        window.onClose_({

            if(UScoreEditorGUI.current == this) {
                UScoreEditorGUI.current = nil
            };
            topBar.remove;
            scoreView.remove;
            tranportBar.remove;
            {
                if( (this.score.events.size != 0) && (this.score.isDirty) ) {
                    SCAlert( "Do you want to save your score? (" ++ this.score.name ++ ")" ,
                        [ [ "Don't save" ], [ "Cancel" ], [ "Save" ],[ "Save as"] ],
                        [ 	nil,
                            { UScoreEditorGUI(scoreEditor) },
                            { this.score.save(nil, {UScoreEditorGUI(scoreEditor)} ) },
                            { this.score.saveAs(nil,nil, {UScoreEditorGUI(scoreEditor)} ) }
                        ] );
                };
            }.defer(0.1)
        });
        //for 3.5 this has to be changed.
        if(window.respondsTo(\drawFunc_)) {
            window.drawFunc_({ current = this });
        } {
            window.drawHook_({ current = this });
        };

        margin = 4;
        gap = 2;
        topBarH = 22;
        tranBarH = 22;
        view = window.view;
        view.background_( Color.grey(0.5) );
        view.addFlowLayout(margin@margin,gap@gap);
        view.resize_(5);

        centerBounds = Rect(0,0, 680-8, 300-( topBarH + tranBarH + (2*margin) + (2*gap) ));
        //centerView = CompositeView(view, centerBounds).resize_(5);
        scoreView = UScoreView(view, centerBounds, scoreEditor );
        
        //TOP
        topBar = UScoreEditorGui_TopBar(view,Rect(0,0, bounds.width-(2*margin), topBarH ),scoreView);
        view.decorator.nextLine;
        
        //CENTER
        scoreView.makeView;
        view.decorator.nextLine;
        
        //BOTTOM
        tranportBar = UScoreEditorGui_TransportBar(view,  Rect(0,0, bounds.width - (2*margin), tranBarH ), scoreView);
	}
}	

+ UScore {
	gui {
		^UScoreEditorGUI( UScoreEditor( this ) );
	}
}	