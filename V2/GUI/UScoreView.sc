
/*

  View that shows events of a score as rectangles with rounded corners.

*/
UScoreView {

     var <scoreEditorsList;
     var <usessionMouseEventsManager;
     var <>snapActive, <>snapH;
     var <scoreView, <scoreListView, <mainComposite, font;
     var <>scoreList;

     *new{ |parent, bounds, scoreEditor| ^super.new.init(scoreEditor, parent,bounds) }

     init { |scoreEditor, parent, bounds|

        scoreEditorsList = [scoreEditor];
        mainComposite = CompositeView(parent,bounds).resize_(5);
        snapActive = true;
		snapH = 0.25;
        font = Font( Font.defaultSansFace, 11 );
		scoreList = [scoreEditor.score];

		this.makeScoreView;
     }

     remake{
        scoreView.remove;
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
        this.remake;
     }

     makeScoreListView{
        var listSize = scoreList.size;
        scoreListView = CompositeView(mainComposite,Rect(0,0,mainComposite.bounds.width,24));
        scoreListView.addFlowLayout;
        scoreList[..(listSize-2)].do{ |score,i|
            SmoothButton(scoreListView,60@16)
                .states_([["score "++(i+1), Color.black, Color.clear]])
                .font_( font )
			    .border_(1).background_(Color.grey(0.8))
			    .radius_(5)
			    .canFocus_(false)
			    .action_({
                       scoreList = scoreList[..i];
                       scoreEditorsList = scoreEditorsList[..i];
                       fork{ { this.remake; }.defer }
			    })
            }
     }

     makeScoreView{
        var score = scoreList.last;
        var scoreEditor = scoreEditorsList.last;
        var numTracks = ((score.events.collect( _.track ).maxItem ? 14) + 2).max(16);
        var scoreBounds = if(scoreList.size > 1) {
            mainComposite.bounds.copy.height_(mainComposite.bounds.height - 24).moveTo(0,24);
        }  {
            mainComposite.bounds
        };

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

		usessionMouseEventsManager = UScoreEditorGuiMouseEventsManager(scoreEditor, this, scoreView.fromBounds.width);

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
					scoreEditor.deleteEvents( usessionMouseEventsManager.selectedEvents )
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

     refresh{ scoreView.refresh }
}