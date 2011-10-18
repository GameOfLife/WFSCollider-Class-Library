
/*

  View that shows events of a score as rectangles with rounded corners.

*/
UScoreView {

     var <scoreEditorsList;
     var <usessionMouseEventsManager;
     var <>snapActive, <>snapH;
     var <>numTracks = 16;
     var <scoreView, <scoreListView, <mainComposite, font, <parent, <bounds;
     var <>scoreList;
     var <currentScoreEditorController, <scoreController;

     *new{ |parent, bounds, scoreEditor| ^super.new.init(scoreEditor, parent,bounds) }

     init { |scoreEditor, inParent, inBounds|

        scoreEditorsList = [scoreEditor];
        snapActive = true;
		snapH = 0.25;
        font = Font( Font.defaultSansFace, 11 );
		scoreList = [scoreEditor.score];
		parent = inParent;
		bounds = inBounds;
		this.addCurrentScoreControllers;

     }

     addCurrentScoreControllers {

         if(this.isInnerScore){
            if( currentScoreEditorController.notNil ) {
                currentScoreEditorController.remove;
            };
            currentScoreEditorController = SimpleController( scoreEditorsList.last );
            currentScoreEditorController.put(\preparingToChangeScore, {
                    this.baseEditor.storeUndoState;
            });
        };


        if( scoreController.notNil ) {
	        scoreController.remove;
	    };
        scoreController = SimpleController( scoreEditorsList.last.score );

        scoreController.put(\pos, {
		    { this.update }.defer;
		});

		scoreController.put(\something, {
		    this.update;
		    if(this.isInnerScore){
		        this.baseEditor.changed(\score);
		    }
		});
	}

	remove {
        [currentScoreEditorController, scoreController, usessionMouseEventsManager].do(_.remove)
    }

	update {
	    scoreView.refresh;
	}

    currentEditor{
        ^scoreEditorsList.last
    }

    baseEditor{
        ^scoreEditorsList[0]
    }

    currentScore{
        ^scoreEditorsList.last.score
    }

    isInnerScore{
        ^(scoreEditorsList.size > 1)
    }

    selectedEvents{ ^usessionMouseEventsManager.selectedEvents }
    selectedEventsOrAll { ^usessionMouseEventsManager.selectedEventsOrAll }

    editSelected{
        var event, events = this.selectedEvents;
        switch(events.size)
            {0}{}
            {1}{
                event = events[0];
                if(event.isFolder){
                    MassEditUChain(event.getAllUChains).gui
                } {
                    event.gui
                }
            }
            { MassEditUChain(events.collect(_.getAllUChains).flat).gui }

    }

	deleteSelected{
	    ^this.currentEditor.deleteEvents(this.selectedEvents)
	}

	selectAll {
	    usessionMouseEventsManager.eventViews.do(_.selected = true);
	    this.update;
	}

    selectSimilar {
        var selectedTypes = this.selectedEvents.collect{ |x| x.units.collect(_.defName) };
        usessionMouseEventsManager.eventViews.do{ |evView|
            if( selectedTypes.includesEqual(evView.event.units.collect(_.defName)) ) {
                evView.selected = true
            }
        };
        this.update
    }

    muteSelected {
        this.currentEditor.muteEvents(this.selectedEvents)
    }

    unmuteSelected {
        this.currentEditor.unmuteEvents(this.selectedEvents)
    }

    soloSelected {
        this.currentEditor.soloEvents(this.selectedEvents)
    }

    duplicateSelected {
        this.currentEditor.duplicateEvents(this.selectedEvents)
    }

    addTrack {
        numTracks = numTracks + 1;
        this.update;
    }

    removeUnusedTracks {
        numTracks = ((this.currentScore.events.collect( _.track )
            .maxItem ? 14) + 2).max( 16 );
        this.update;
    }

    openSelectedSubScoreInNewWindow{
        this.selectedEvents !! { |x|
            var y = x.at(0);
            if(y.isFolder) {
                UScoreEditorGUI(UScoreEditor(y))
            }
        }

    }


     // call to initialize and draw view. this mean the views are only actually drawn when this method is called after creating this instance.
     // This is needed to be able to pass an instance of this class to the topbar object.
    makeView{
        mainComposite = CompositeView(parent,bounds).resize_(5);
        this.makeScoreView
    }

     remake{

        if(scoreListView.notNil){
            scoreListView.remove;
            scoreListView = nil
        };
        usessionMouseEventsManager.remove;
        if(scoreList.size > 1) {
            this.makeScoreListView;
        };
		this.makeScoreView;
     }

     addtoScoreList{ |score|
        scoreList = scoreList.add(score);
        scoreEditorsList = scoreEditorsList.add(UScoreEditor(score));
        this.addCurrentScoreControllers;
        this.remake;
        this.changed(\activeScoreChanged);
     }

     goToHigherScore{ |i|
        scoreList = scoreList[..i];
        scoreEditorsList = scoreEditorsList[..i];
        this.addCurrentScoreControllers;
        this.changed(\activeScoreChanged);
        fork{ { this.remake; }.defer }
     }

     makeScoreListView{
        var listSize = scoreList.size;
        scoreListView = CompositeView(mainComposite,Rect(0,0,mainComposite.bounds.width,24));
        scoreListView.addFlowLayout;
        scoreList[..(listSize-2)].do{ |score,i|
            SmoothButton(scoreListView,60@16)
                .states_([[(i+1).asString++": "++score.name, Color.black, Color.clear]])
                .font_( font )
			    .border_(1).background_(Color.grey(0.8))
			    .radius_(5)
			    .canFocus_(false)
			    .action_({
                    this.goToHigherScore(i);
			    })
        };
        SmoothButton(scoreListView,16@16)
                .states_([[\up, Color.black, Color.clear]])
                .font_( font )
			    .border_(1).background_(Color.grey(0.8))
			    .radius_(5)
			    .canFocus_(false)
			    .action_({
                    UScoreEditorGUI( UScoreEditor( this.currentScore ) )
			    })

     }

     makeScoreView{
        var scoreEditor = scoreEditorsList.last;
        var score = scoreEditor.score;
        var scoreBounds = if(scoreList.size > 1) {
            mainComposite.bounds.copy.height_(mainComposite.bounds.height - 24).moveTo(0,24);
        }  {
            mainComposite.bounds.copy.moveTo(0,0)
        };

        if(scoreView.notNil) {
            scoreView.view.visible_(false);
            scoreView.view.focus(false);
            scoreView.remove;
        };

        numTracks = ((score.events.collect( _.track ).maxItem ? 14) + 2).max(16);

        scoreView = ScaledUserViewContainer(mainComposite,
        			scoreBounds,
        			Rect( 0, 0, score.duration.ceil.max(1), numTracks ),
        			5);

        //CONFIGURE scoreView
        scoreView.background = Color.gray(0.8);
        scoreView.composite.resize = 5;
	    scoreView.gridLines = [score.finiteDuration.ceil.max(1), numTracks];
		scoreView.gridMode = ['blocks','lines'];
		scoreView.sliderWidth = 8;
		//scoreView.maxZoom = [16,5];

		usessionMouseEventsManager = UScoreEditorGuiMouseEventsManager(this);

		scoreView
			.mouseDownAction_( { |v, x, y,mod,x2,y2, isInside, buttonNumber, clickCount| 	 // only drag when one event is selected for now

				var scaledPoint, shiftDown,altDown;

        		scaledPoint = [ x,y ].asPoint;
				shiftDown = ModKey( mod ).shift( \only );
				altDown = ModKey( mod ).alt( \only );

				usessionMouseEventsManager.mouseDownEvent(scaledPoint,Point(x2,y2),shiftDown,altDown,v,clickCount);

			} )
			.mouseMoveAction_( { |v, x, y, mod, x2, y2, isInside, buttonNumber|
				var snap = if(snapActive){snapH * v.gridSpacingH}{0};
				var shiftDown = ModKey( mod ).shift( \only );

				usessionMouseEventsManager.mouseMoveEvent(Point(x,y),Point(x2,y2),v,snap, shiftDown, v.fromBounds.width);

			} )
			.mouseUpAction_( { |v, x, y, mod, x2, y2, isInside, buttonNumber, clickCount|

				var shiftDown = ModKey( mod ).shift( \only );

				usessionMouseEventsManager.mouseUpEvent(Point(x,y),Point(x2,y2),shiftDown,v,isInside);

			} )
			.keyDownAction_( { |v, a,b,c|
				if( c == 127 ) {
					this.deleteSelected
				};
				if( c == 32 ) {
				    if(score.isStopped) {
				        score.prepareAndStart( UServerCenter.servers, score.pos, true, score.loop);
				    } {
				        score.stop;
				    }

				}
			})
			.beforeDrawFunc_( {
			    var dur = score.finiteDuration.ceil.max(1);
				numTracks = ((score.events.collect( _.track ).maxItem ? ( numTracks - 2)) + 2)
					.max( numTracks );
				scoreView.fromBounds = Rect( 0, 0, dur, numTracks );
				scoreView.gridLines = [dur, numTracks];
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
					eventView.draw(v, v.fromBounds.width );
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
				scPos = v.translateScale( score.pos@0 );
				Pen.line( (scPos.x)@0, (scPos.x)@v.bounds.height);
				Pen.stroke;

				Pen.width = 1;
				Color.grey(0.5,1).set;
				Pen.strokeRect( rect.insetBy(0.5,0.5) );

		})
     }

     refresh{ scoreView.refresh; scoreView.refresh }
}