UServerCenter{
    classvar <>servers;

    *initClass {
        servers = [Server.default]
    }
}

UScoreEditorGUI {
	
	classvar <>current, <all;

	var <scoreEditor;

	var <>userView, <>window;
	var <>snapActive, <>snapH, <>numTracks;
	var <usessionMouseEventsManager;
	var <scoreEditorController, <scoreController;

	//*initClass { UI.registerForShutdown({ scoreEditor.askForSave = false }); }

	*new { |scoreEditor|
		^super.newCopyArgs( scoreEditor)
			.init
			.addToAll
			.newWindow
	}

	init {
		snapActive = true;
		snapH = 0.25;
		numTracks = 16;
		usessionMouseEventsManager = UScoreEditorGuiMouseEventsManager(scoreEditor);
		this.addControllers;
	}

	addControllers {
	    scoreEditorController = SimpleController( scoreEditor );

		scoreEditorController.put(\score, {
		    this.update
		});

        scoreController = SimpleController( scoreEditor.score );

        scoreController.put(\pos, {
		    { this.update }.defer;
		});

	}

	update {
		if( window.isClosed.not ) {
			userView.refresh;
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

	removeFromAll { if( all.notNil ) { all.remove( this ); UTransport.refreshScoreMenu; }; }

    /*
	createWindowTitle{
		^"scoreEditor ( "++
		if( isMainEditor ) {
			scoreEditor.all.indexOf(this).asString++" - "++
			if( scoreEditor.filePath.isNil ) {
				"Untitled )"
			} {
				PathName(scoreEditor.filePath).fileName.removeExtension++" )"
			}
		} {
			if( parent.id.notNil ) {
				("folder of " ++ ( parent !? { parent.id } ))++": " ++ scoreEditor.name ++ " )"
			} {
				"folder: " ++ scoreEditor.name ++ " )"
			}
		};
	}

	setWindowTitle{
		window.window.name = this.createWindowTitle;
	}
    */

	newWindow {

		var font = Font( Font.defaultSansFace, 11 ), header, windowTitle, margin, gap, topBarH, tranBarH, view;
        var bounds = Rect(230 + 20.rand2, 230 + 20.rand2, 680, 300);
		numTracks = ((scoreEditor.events.collect( _.track ).maxItem ? 14) + 2).max(16);

        window = Window(/*this.createWindowTitle*/"scoreEditor", bounds).front;

		window.onClose = { scoreEditorController.remove };

        margin = 4;
        gap = 2;
        topBarH = 22;
        tranBarH = 22;
        view = window.view;
        view.background_( Color.grey(0.5) );
        view.addFlowLayout(margin@margin,gap@gap);
        view.resize_(5);
        
        //TOP
        UScoreEditorGui_TopBar(view,Rect(0,0, bounds.width-(2*margin), topBarH ),scoreEditor,usessionMouseEventsManager, this);
        view.decorator.nextLine;
        
        //CENTER
        userView = ScaledUserView(view,
        			Rect(0,0, 680-8, 300-( topBarH + tranBarH + (2*margin) + (2*gap) )),
        			Rect( 0, 0, scoreEditor.score.duration.ceil.max(1), numTracks ) );

        view.decorator.nextLine;
        
        //BOTTOM
        UScoreEditorGui_TransportBar(view,  Rect(0,0, bounds.width - (2*margin), tranBarH ), scoreEditor.score);

        //CONFIGURE USERVIEW
        userView.background = Color.gray(0.8);
        userView.view.resize = 5;
	    userView.gridLines = [scoreEditor.score.duration.ceil,max(1), numTracks];
		userView.gridMode = ['blocks','lines'];
		//userView.maxZoom = [16,5];

		userView
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
					scoreEditor.deleteEvents( usessionMouseEventsManager.selectedEvents )
				}
			})
			.beforeDrawFunc_( {
				numTracks = ((scoreEditor.events.collect( _.track ).maxItem ? ( numTracks - 2)) + 2)
					.max( numTracks );
				userView.fromBounds = Rect( 0, 0, scoreEditor.score.duration.ceil.max(1), numTracks );
				userView.gridLines = [scoreEditor.score.duration.ceil.max(1), numTracks];
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
				scPos = v.translateScale( scoreEditor.score.pos@0 );
				Pen.line( (scPos.x)@0, (scPos.x)@v.bounds.height);
				Pen.stroke;	
				
				Pen.width = 1;
				Color.grey(0.5,1).set;
				Pen.strokeRect( rect.insetBy(0.5,0.5) );
						
		})
							
	}
}		