UScoreEditorGui_TopBar {

    var <>scoreView;
    var <header, <>views, <scoreEditorController, <scoreController;

    *new{ |parent, bounds, scoreView|
        ^super.newCopyArgs(scoreView).init(parent, bounds)
    }

    init{ |parent, bounds|
        this.makeGui(parent, bounds);
        this.addScoreEditorController;
    }

    remove{
        if(scoreEditorController.notNil) {
                scoreEditorController.remove;
        };
        if( scoreController.notNil ) {
	        scoreController.remove;
	    };
        header.remove;
    }

    scoreEditor{
         ^scoreView.currentEditor
    }

    addScoreEditorController{
        if(scoreEditorController.notNil) {
                scoreEditorController.remove;
        };
        scoreEditorController = SimpleController( scoreView.currentEditor );

		scoreEditorController.put(\undo, {

            if( this.scoreEditor.redoStates.size != 0 ) {
                views[\redo].enabled_(true)
            };
            if( this.scoreEditor.undoStates.size == 0 ) {
                views[\undo].enabled_(false)
            };
        });

        scoreEditorController.put(\redo, {

            if( this.scoreEditor.undoStates.size != 0 ) {
			views[\undo].enabled_(true)
            };
            if( this.scoreEditor.redoStates.size == 0 ) {
                views[\redo].enabled_(false)
            }
        });

        if( scoreController.notNil ) {
	        scoreController.remove;
	    };
        scoreController = SimpleController( scoreView.currentScore );

        scoreController.put(\something, {
		    views[\undo].enabled_(true);
		    views[\redo].enabled_(false);
		});

    }

    resetUndoRedoButtons{
        views[\redo].enabled_(this.scoreEditor.redoStates.size != 0);
        views[\undo].enabled_(this.scoreEditor.undoStates.size != 0);
    }

    selectedEvents{
        ^scoreView.selectedEvents;
    }

    selectedEventsOrAll{
        ^scoreView.selectedEventsOrAll
    }

    makeGui{ |parent, bounds|
        var font = Font( Font.defaultSansFace, 11 ), size, marginH, marginV;
		views = ();
		
	    marginH = 2;
	    marginV = 2;
		size = bounds.height - (2*marginV);
		
        header = CompositeView( parent, bounds );
        
		header.addFlowLayout(marginH@marginV);
		header.resize_(2);

		SmoothButton( header, size@size )
			.states_( [[ \i, Color.black, Color.blue.alpha_(0.125) ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({ |b|
				scoreView.editSelected;
			});

		header.decorator.shift(10);

		SmoothButton( header, size@size )
			.states_( [[ '-' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({
				scoreView.deleteSelected;
			});

		SmoothButton( header, size@size )
			.states_( [[ '+' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    if(scoreView.selectedEvents.notNil) {
				    scoreView.duplicateSelected;
				} {
				    scoreView.currentEditor.addEvent
				}
			});

		header.decorator.shift(10);

		SmoothButton( header, size@size  )
 			.states_( [[ "[", Color.black, Color.clear ]] )
 			.canFocus_(false)
			.radius_( 0 )
			.border_(1).background_(Color.grey(0.8))
			.font_( Font( font.name, 10 ).boldVariant )
			.radius_([8,0,0,8])
			.action_({
				this.selectedEventsOrAll !! { |x| this.scoreEditor.trimEventsStartAtPos( x ) }
			});

		SmoothButton( header, size@size  )
			.states_( [[ "|", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_(0)
			.border_(1).background_(Color.grey(0.8))
			.action_({
				this.selectedEventsOrAll !! { |x| this.scoreEditor.splitEventsAtPos( x ) }
			});

		SmoothButton( header, size@size  )
			.states_( [[ "]", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_([0,8,8,0])
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    this.selectedEventsOrAll !! { |x| this.scoreEditor.trimEventsEndAtPos( x ) }
		    });

		header.decorator.shift(10);

		views[\undo] = SmoothButton( header, size@size )
			.states_( [[ 'arrow_pi' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.enabled_(false)
			.action_({
				this.scoreEditor.undo
			});

		views[\redo] = SmoothButton( header, size@size )
			.states_( [[ 'arrow' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.enabled_(false)
			.action_({
				this.scoreEditor.redo
			});

		header.decorator.shift(10);

		SmoothButton( header, size@size  )
			.states_( [[ \speaker, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({ |b|
				this.selectedEvents !! { |x|  this.scoreEditor.toggleDisableEvents( x ) }
			});

		SmoothButton( header, size@size  )
			.states_( [[ \folder, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    this.selectedEvents !! { |x|
                    if( x.every(_.isFolder) ) {
                        this.scoreEditor.unpackSelectedFolders(x)
                    }{
                        this.scoreEditor.folderFromEvents(x);
                    }
				}
			});

		header.decorator.shift(10);

		SmoothButton( header, 40@size  )
			.states_( [[ "mixer", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({ |b|
				UMixer(scoreView.currentScore);
			});


		header.decorator.shift(100);

		StaticText( header, 30@size ).string_( "snap" ).font_( font ).align_( \right );

		PopUpMenu( header, 50@size )
			.items_( [ "off", "0.001", "0.01", "0.1", "0.25", "0.333", "1" ] )
			.canFocus_(false)
			.font_( font )
			.value_(4)
			.action_({ |v|
				if (v.value == 0)
					{ scoreView.snapActive = false; }
					{ scoreView.snapActive = true; };

				scoreView.snapH = [0, 0.001, 0.01, 0.1, 0.25, 1/3, 1][ v.value ];
				});

		StaticText( header, 10@size ).string_( "s" ).font_( font );

		header.decorator.shift(4);

		StaticText( header, 30@size ).string_( "Mode:" ).font_( font );

		PopUpMenu( header, 50@size )
			.items_( [ "all","move","resize","fades"] )
			.canFocus_(false)
			.font_( font )
			.value_(0)
			.action_({ |v|
				scoreView.usessionMouseEventsManager.mode = v.items[v.value].asSymbol;
			});

    }

}