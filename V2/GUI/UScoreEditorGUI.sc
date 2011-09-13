UServerCenter{
    classvar <>servers;

    *initClass {
        servers = [Server.default]
    }
}

UScoreEditorGUI {
	
	classvar <>current, <all;

	var <uscoreEditor;

	var <>window;
	var <snapActive, <>snapH, <>numTracks;
	var <usessionMouseEventsManager;
	var views;
	var <scoreEditorController, <scoreController;

	//*initClass { UI.registerForShutdown({ UScoreEditor.askForSave = false }); }

	*new { |uscoreEditor|
		^super.newCopyArgs( uscoreEditor)
			.init
			.addToAll
			.newWindow
	}

	init {
		snapActive = true;
		snapH = 0.25;
		numTracks = 16;
		usessionMouseEventsManager = UScoreEditorGuiMouseEventsManager(uscoreEditor);
		this.addControllers;
	}

	addControllers {
	    scoreEditorController = SimpleController( uscoreEditor );

		scoreEditorController.put(\score, {
		    views[\undo].enabled_(true);
		    views[\redo].enabled_(false);
		    this.update
		});

		scoreEditorController.put(\undo, {

            if( uscoreEditor.redoStates.size != 0 ) {
                views[\redo].enabled_(true)
            };
            if( uscoreEditor.undoStates.size == 0 ) {
                views[\undo].enabled_(false)
            };
        });

        scoreEditorController.put(\redo, {

            if( uscoreEditor.undoStates.size != 0 ) {
			views[\undo].enabled_(true)
            };
            if( uscoreEditor.redoStates.size == 0 ) {
                views[\redo].enabled_(false)
            }
        });

        scoreController = SimpleController( uscoreEditor.score );

        scoreController.put(\pos, {
		    this.update
		});

	}

	update {
		if( window.window.notNil && { window.window.dataptr.notNil } ) {
			window.refresh;
		};
	}

	toFront {
	    if( window.window.notNil && { window.window.dataptr.notNil } ) {
	     window.window.front;
	    };
	}

	addToAll {
		all = all.asCollection.add( this );
	}

	removeFromAll { if( all.notNil ) { all.remove( this ); UTransport.refreshScoreMenu; }; }

	selectedEvents { ^usessionMouseEventsManager.selectedEvents  }

	selectedEventsOrAll {
		var events = this.selectedEvents;
		if(events.size == 0){
			^uscoreEditor.events
		} {
			^events
		}
	}
    /*
	createWindowTitle{
		^"UScoreEditor ( "++
		if( isMainEditor ) {
			UScoreEditor.all.indexOf(this).asString++" - "++
			if( uscoreEditor.filePath.isNil ) {
				"Untitled )"
			} {
				PathName(uscoreEditor.filePath).fileName.removeExtension++" )"
			}
		} {
			if( parent.id.notNil ) {
				("folder of " ++ ( parent !? { parent.id } ))++": " ++ uscoreEditor.name ++ " )"
			} {
				"folder: " ++ uscoreEditor.name ++ " )"
			}
		};
	}

	setWindowTitle{
		window.window.name = this.createWindowTitle;
	}
    */

	newWindow {

		var font = Font( Font.defaultSansFace, 11 ), header, windowTitle;
		views = ();

		numTracks = ((uscoreEditor.events.collect( _.track ).maxItem ? 14) + 2).max(16);

		window = ScaledUserView.window(/*this.createWindowTitle*/"testing",
			Rect(230 + 20.rand2, 230 + 20.rand2, 680, 300),
			fromBounds: Rect( 0, 0, uscoreEditor.score.duration.ceil.max(1), numTracks ),
			viewOffset: [4, 27] );

		window.userView.background = Color.gray(0.8);

		window.onClose = { scoreEditorController.remove };

	    window.userView.gridLines = [uscoreEditor.score.duration.ceil,max(1), numTracks];
		window.userView.gridMode = ['blocks','lines'];
		window.maxZoom = [16,5];

		//window.window.acceptsMouseOver_( true );

		header = CompositeView( window.window, Rect(0,0, window.window.view.bounds.width, 25 ) );
		header.addFlowLayout;
		//header.background_( Color.gray(0.95) );
		//header.resize_(2);

		SmoothButton( header, 18@18 )
			.states_( [[ \i, Color.black, Color.blue.alpha_(0.125) ]] )
			.canFocus_(false)
			.border_(1)
			.border_(1)
			.action_({ |b|
				this.editSelected(true)
			});

		header.decorator.shift(10);

		SmoothButton( header, 18@18 )
			.states_( [[ '-' ]] )
			.canFocus_(false)
			.border_(1)
			.action_({
				uscoreEditor.deleteEvents( this.selectedEvents )
			});

		SmoothButton( header, 18@18 )
			.states_( [[ '+' ]] )
			.canFocus_(false)
			.border_(1)
			.action_({
				if( this.selectedEvents.size > 0 )
					{ uscoreEditor.duplicateEvents(this.selectedEvents) }
					//{ this.addAudioFiles }
			});

		header.decorator.shift(10);

		SmoothButton( header, 18@18  )
 			.states_( [[ "[", Color.black, Color.clear ]] )
 			.canFocus_(false)
			.radius_( 0 )
			.border_(1)
			.font_( Font( font.name, 10 ).boldVariant )
			.radius_([8,0,0,8])
			.action_({
				uscoreEditor.trimEventsStartAtPos( this.selectedEventsOrAll )
			});

		SmoothButton( header, 18@18  )
			.states_( [[ "|", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_(0)
			.border_(1)
			.action_({
				uscoreEditor.splitEventsAtPos( this.selectedEventsOrAll )
			});

		SmoothButton( header, 18@18  )
			.states_( [[ "]", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_([0,8,8,0])
			.border_(1)
			.action_({
			    uscoreEditor.trimEventsEndAtPos( this.selectedEventsOrAll )
		    });

		header.decorator.shift(10);

		views[\undo] = SmoothButton( header, 18@18 )
			.states_( [[ 'arrow_pi' ]] )
			.canFocus_(false)
			.border_(1)
			.enabled_(false)
			.action_({
				uscoreEditor.undo
			});

		views[\redo] = SmoothButton( header, 18@18 )
			.states_( [[ 'arrow' ]] )
			.canFocus_(false)
			.border_(1)
			.enabled_(false)
			.action_({
				uscoreEditor.redo
			});

		header.decorator.shift(10);

		SmoothButton( header, 18@18  )
			.states_( [[ \speaker, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1)
			.action_({ |b|
				uscoreEditor.toggleMuteEvents( this.selectedEvents )
			});

		SmoothButton( header, 18@18  )
			.states_( [[ \folder, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1)
			.action_({
				if( this.selectedEvents.every(_.isFolder) ) {
					this.unpackSelectedFolders
				}{
					this.folderFromSelectedEvents;
				};
			});

		header.decorator.shift(10);

		SmoothButton( header, 40@18  )
			.states_( [[ "mixer", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({ |b|
				// UMixer doesn't exist yet
				//UMixer(this.selectedEventsOrAll,List.new);
			});

		/*
		// there should be a better way to do this
		SmoothButton( header, 40@18  )
			.states_( [[ "batch", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({
				UBatch.new
			}); */

		header.decorator.shift(10);
        /*
		SmoothButton( header, 40@18  )
			.states_( [[ "plot", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({
			    //not implemented yet
				uscoreEditor.score.plot;
			});

		SmoothButton( header, 50@18  )
			.states_( [[ "plot all", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({
			    //not implemented yet
				uscoreEditor.score.plot(all);
			});

		header.decorator.shift(10);
        */
		SmoothButton( header, 40@18  )
			.states_( [
			    [ \play, Color.black, Color.clear ],
			    [ \stop, Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({  |v|
			    if( v.value == 1) {
			        uscoreEditor.score.start( UServerCenter.servers, uscoreEditor.score.pos);
			    } {
                    uscoreEditor.score.release;
                    views[\pause].value = 0;
			    }

			});

		views[\pause] = SmoothButton( header, 50@18  )
			.states_( [
			    [ \pause, Color.black, Color.clear ],
			    [ \pause, Color.blue, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({ |v|
			    if( v.value == 1) {
			        uscoreEditor.score.pause
			    } {
                    uscoreEditor.score.resume(UServerCenter.servers)
			    }
			});

		views[\pause] = SmoothButton( header, 50@18  )
			.states_( [[ "<<", Color.black, Color.clear ]])
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({
			    uscoreEditor.score.stoppedAt = 0
			});

		StaticText( header, 30@18 ).string_( "snap" ).font_( font ).align_( \right );

		PopUpMenu( header, 50@18 )
			.items_( [ "off", "0.001", "0.01", "0.1", "0.25", "0.333", "1" ] )
			.canFocus_(false)
			.font_( font )
			.value_(4)
			.action_({ |v|
				if (v.value == 0)
					{ snapActive = false; }
					{ snapActive = true; };

				snapH = [0, 0.001, 0.01, 0.1, 0.25, 1/3, 1][ v.value ];
				});

		StaticText( header, 10@18 ).string_( "s" ).font_( font );

		header.decorator.shift(4);

		StaticText( header, 30@18 ).string_( "Mode:" ).font_( font );

		PopUpMenu( header, 50@18 )
			.items_( [ "all","move","resize","fades"] )
			.canFocus_(false)
			.font_( font )
			.value_(0)
			.action_({ |v|
				usessionMouseEventsManager.mode = v.items[v.value].asSymbol;
			});


		window.userView
			.mouseDownAction_( { |v, x, y,mod,x2,y2| 	 // only drag when one event is selected for now
				var scaledPoint, shiftDown,altDown;

				scaledPoint = [ x,y ].asPoint;
				shiftDown = ModKey( mod ).shift( \only );
				altDown = ModKey( mod ).alt( \only );

				usessionMouseEventsManager.mouseDownEvent(scaledPoint,Point(x2,y2),shiftDown,altDown,v);

			} )
			.mouseMoveAction_( { |v, x, y, mod, x2, y2, isInside|
				var snap = if(snapActive){snapH * v.gridSpacingH}{0};
				var shiftDown = ModKey( mod ).shift( \only );

				usessionMouseEventsManager.mouseMoveEvent(Point(x,y),Point(x2,y2),v,snap, shiftDown);

			} )
			.mouseUpAction_( { |v, x, y, mod, x2, y2, isInside|

				var shiftDown = ModKey( mod ).shift( \only );

				usessionMouseEventsManager.mouseUpEvent(Point(x,y),Point(x2,y2),shiftDown,v,isInside);

			} )
			.keyDownAction_( { |v, a,b,c|
				if( c == 127 ) {
					uscoreEditor.deleteEvents( this.selectedEvents )
				}
			})
			.beforeDrawFunc_( {
				numTracks = ((uscoreEditor.events.collect( _.track ).maxItem ? ( numTracks - 2)) + 2)
					.max( numTracks );
				window.userView.fromBounds = Rect( 0, 0, uscoreEditor.score.duration.ceil.max(1), numTracks );
				window.userView.gridLines = [uscoreEditor.score.duration.ceil.max(1), numTracks];
				} )

			.unscaledDrawFunc_( { |v|
				var scPos, rect;
				rect = v.view.drawBounds.moveTo(0,0);
				//draw border
				GUI.pen.use({	 
					GUI.pen.addRect( rect.insetBy(0.5,0.5) );
					GUI.pen.fillColor = Color.gray(0.7).alpha_(0.5);
					GUI.pen.strokeColor = Color.gray(0.1).alpha_(0.5);
					GUI.pen.fill;
				});
				
				Pen.font = Font( Font.defaultSansFace, 10 );												
				//draw events
				usessionMouseEventsManager.eventViews.do({ |eventView|
					eventView.draw(v);
				});	
				
				//draw selection rectangle
				if(usessionMouseEventsManager.selectionRect.notNil) {
					Pen.color = Color.white;
					Pen.addRect(v.translateScale(usessionMouseEventsManager.selectionRect));
					Pen.stroke;					
					Pen.color = Color.grey(0.3).alpha_(0.4);
					Pen.addRect(v.translateScale(usessionMouseEventsManager.selectionRect));
					Pen.fill;
				};
				
				//draw Transport line
				Pen.width = 2;			
				Pen.color = Color.black.alpha_(0.5);
				scPos = v.translateScale( uscoreEditor.score.pos@0 );
				Pen.line( (scPos.x)@0, (scPos.x)@v.bounds.height);
				Pen.stroke;	
				
				Pen.width = 1;
				Color.grey(0.5,1).set;
				Pen.strokeRect( rect.insetBy(0.5,0.5) );
							
						
		})
							
	}
}		