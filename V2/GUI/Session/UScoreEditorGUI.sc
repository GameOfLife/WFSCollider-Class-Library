UScoreEditorGUI : UAbstractWindow {

	var <scoreEditor;

	var <>scoreView, <tranportBar, topBar;
	var <usessionMouseEventsManager;
	var <scoreController;

	//*initClass { UI.registerForShutdown({ scoreEditor.askForSave = false }); }

	*new { |scoreEditor, bounds|
		^super.new.init( scoreEditor)
			.addToAll
			.makeGui(bounds)
	}

	*currentSelectedEvents{
	    ^this.current.selectedEvents
	}

    init { |inScoreEditor|
        scoreEditor = if(inScoreEditor.class == UScore) {
            UScoreEditor(inScoreEditor)
        } {
            inScoreEditor;
        };
        scoreController = SimpleController(scoreEditor.score);
        scoreController.put(\name,{
            window.name = this.windowTitle
        })
    }

	score { ^scoreEditor.score }
	editor { ^scoreEditor }
	currentScore { ^scoreEditor.currentScore }
	currentEditor { ^scoreEditor.currentEditor }
	selectedEvents{ ^scoreView.selectedEvents }

    windowTitle {
        ^("Score Editor : "++this.score.name)
    }

    remove {
        scoreController.remove;
    }
	makeGui { |bounds|

		var font = Font( Font.defaultSansFace, 11 ), header, windowTitle, margin, gap, topBarH, tranBarH, centerView, centerBounds;

        margin = 4;
        gap = 2;

        this.newWindow(bounds, this.windowTitle,{

            if(UScoreEditorGUI.current == this) {
                UScoreEditorGUI.current = nil
            };
            this.remove;
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
        }, margin:margin, gap:gap);
        view.addFlowLayout(margin@margin,gap@gap);
        bounds = window.bounds;
        margin = 4;
        gap = 2;
        topBarH = 22;
        tranBarH = 22;

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