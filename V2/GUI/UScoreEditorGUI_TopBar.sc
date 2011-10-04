UScoreEditorGui_TopBar {

    var <>usessionMouseEventsManager;
    var <>views, <>scoreEditorController;

    *new{ |parent, bounds, scoreEditor, usessionMouseEventsManager, scoreEditorGUI|
        ^super.newCopyArgs(usessionMouseEventsManager).init(parent, bounds, scoreEditor, scoreEditorGUI)
    }

    init{ |parent, bounds, scoreEditor, scoreEditorGUI|
        this.makeGui(parent, bounds, scoreEditor, scoreEditorGUI);
        this.addControllers(scoreEditor);
    }

    addControllers{ |scoreEditor|
        scoreEditorController = SimpleController( scoreEditor );

		scoreEditorController.put(\score, {
		    views[\undo].enabled_(true);
		    views[\redo].enabled_(false);
		});

		scoreEditorController.put(\undo, {

            if( scoreEditor.redoStates.size != 0 ) {
                views[\redo].enabled_(true)
            };
            if( scoreEditor.undoStates.size == 0 ) {
                views[\undo].enabled_(false)
            };
        });

        scoreEditorController.put(\redo, {

            if( scoreEditor.undoStates.size != 0 ) {
			views[\undo].enabled_(true)
            };
            if( scoreEditor.redoStates.size == 0 ) {
                views[\redo].enabled_(false)
            }
        });

    }

    selectedEvents { ^usessionMouseEventsManager.selectedEvents  }

    makeGui{ |parent, bounds, scoreEditor, scoreEditorGUI|
        var font = Font( Font.defaultSansFace, 11 ), header, size, marginH, marginV;
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
				var events = usessionMouseEventsManager.selectedEvents;
				switch(events.size)
				    {0}{}
				    {1}{ events[0].gui }
				    { MassEditUChain(events.collect(_.object).select{ |x| x.class == UChain}).gui }

			});

		header.decorator.shift(10);

		SmoothButton( header, size@size )
			.states_( [[ '-' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({
				scoreEditor.deleteEvents( usessionMouseEventsManager.selectedEvents )
			});

		SmoothButton( header, size@size )
			.states_( [[ '+' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({
				if( usessionMouseEventsManager.selectedEvents.size > 0 )
					{ scoreEditor.duplicateEvents(usessionMouseEventsManager.selectedEvents) }
					//{ this.addAudioFiles }
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
				scoreEditor.trimEventsStartAtPos( usessionMouseEventsManager.selectedEventsOrAll )
			});

		SmoothButton( header, size@size  )
			.states_( [[ "|", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_(0)
			.border_(1).background_(Color.grey(0.8))
			.action_({
				scoreEditor.splitEventsAtPos( usessionMouseEventsManager.selectedEventsOrAll )
			});

		SmoothButton( header, size@size  )
			.states_( [[ "]", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_([0,8,8,0])
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    scoreEditor.trimEventsEndAtPos( usessionMouseEventsManager.selectedEventsOrAll )
		    });

		header.decorator.shift(10);

		views[\undo] = SmoothButton( header, size@size )
			.states_( [[ 'arrow_pi' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.enabled_(false)
			.action_({
				scoreEditor.undo
			});

		views[\redo] = SmoothButton( header, size@size )
			.states_( [[ 'arrow' ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.enabled_(false)
			.action_({
				scoreEditor.redo
			});

		header.decorator.shift(10);

		SmoothButton( header, size@size  )
			.states_( [[ \speaker, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({ |b|
				scoreEditor.toggleMuteEvents( usessionMouseEventsManager.selectedEvents )
			});

		SmoothButton( header, size@size  )
			.states_( [[ \folder, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1).background_(Color.grey(0.8))
			.action_({
				if( usessionMouseEventsManager.selectedEvents.every(_.isFolder) ) {
					usessionMouseEventsManager.unpackSelectedFolders
				}{
					usessionMouseEventsManager.folderFromSelectedEvents;
				};
			});

		header.decorator.shift(10);

		SmoothButton( header, 40@size  )
			.states_( [[ "mixer", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({ |b|
				// UMixer doesn't exist yet
				//UMixer(this.selectedEventsOrAll,List.new);
			});

		header.decorator.shift(10);

		SmoothButton( header, 40@18  )
			.states_( [[ "plot", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1).background_(Color.grey(0.8))
			.action_({
			    //not implemented yet
				//scoreEditor.score.plot;
			});

		header.decorator.shift(10);

		StaticText( header, 30@size ).string_( "snap" ).font_( font ).align_( \right );

		PopUpMenu( header, 50@size )
			.items_( [ "off", "0.001", "0.01", "0.1", "0.25", "0.333", "1" ] )
			.canFocus_(false)
			.font_( font )
			.value_(4)
			.action_({ |v|
				if (v.value == 0)
					{ scoreEditorGUI.snapActive = false; }
					{ scoreEditorGUI.snapActive = true; };

				scoreEditorGUI.snapH = [0, 0.001, 0.01, 0.1, 0.25, 1/3, 1][ v.value ];
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
				usessionMouseEventsManager.mode = v.items[v.value].asSymbol;
			});

    }

}