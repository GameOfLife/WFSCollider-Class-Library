UScoreEditorGuiMouseEventsManager {
	classvar minimumMov = 3;
	var <scoreView;
	var <score, <scoreEditor, <eventViews, <scoreEditorGUI, <>state = \nothing;
	var <mouseMoved = false, <mouseDownPos, <unscaledMouseDownPos, <clickCount;
	var <selectionRect, theEventView;
	var <xLimit, <yLimit;
	var <isCopying = false, copyed = false;
	var <>mode = \all;
	var scoreController;

	//state is \nothing, \moving, \resizingFront, \resizingBack, \selecting, \fadeIn, \fadeOut;
	//mode is \all, \move, \resize, \fades
	//protocol:
	
	//initial click:
	// inside region
	//	- no shift down -> select 
	//	- shift down -> invert selection
	// resize region area
	//   - mouseUp after no movement -> select, no resize
	//   - mouseUp after movement -> don't select, start resize, of all selected
	// outside any region
	//   -> start selecting 
	//     - shiftDown -> add to selection
	//     - no shiftDown -> only set newly selected events
	
	
	*new { |scoreView|
		^super.newCopyArgs(scoreView).init
	}

	init {
	    var maxWidth = scoreView.scoreView.fromBounds.width;
        scoreEditor = scoreView.currentEditor;
        score = scoreView.currentScore;
        this.makeEventViews(maxWidth);
        scoreController = SimpleController( score );

		scoreController.put(\numEventsChanged, {
		    //"rebuilding views".postln;
		    this.makeEventViews(maxWidth)
		});
	}

	remove{
	    scoreController.remove;
	}

	makeEventViews{ |maxWidth|
	    eventViews = scoreEditor.events.collect{ |event,i|
			event.makeView(i,maxWidth)
	    };
	}
	
	isResizing{
		^(state == \resizingFront) || (state == \resizingBack )
	}
	
	isResizingOrFades {
		^(state == \resizingFront) || (state == \resizingBack ) || (state == \fadeIn) || (state == \fadeOut )
	}
	
	selectedEventViews {	
		var events = this.eventViews.select{ |eventView|
			eventView.selected
		};
		^if(events.size > 0){events}{nil}
	}

	selectedEvents {
	    ^this.selectedEventViews.collect( _.event )
	}

	selectedEventsOrAll {
	    var v = this.selectedEventViews;
	    if(v.size > 0){
	        ^v.collect( _.event )
	    } {
	        v = score.events;
	        if(v.size > 0) {
	            ^v
	        } {
	            ^nil
	        }
	    }
	}
		
	mouseDownEvent{ |mousePos,unscaledMousePos,shiftDown,altDown,scaledUserView, inClickCount|
		
		mouseDownPos = mousePos;
		unscaledMouseDownPos = unscaledMousePos;
		clickCount = inClickCount;

		eventViews.do{ |eventView|
			eventView.mouseDownEvent(mousePos,scaledUserView,shiftDown,mode)
		};
		
		theEventView = eventViews.select{ |eventView|
			eventView.state == \resizingFront
		}.at(0);
		
		if(theEventView.notNil){
			state = \resizingFront
		} {
			theEventView = eventViews.select{ |eventView|
				eventView.state == \resizingBack
			}.at(0);
			
			if(theEventView.notNil){
				state = \resizingBack
			} {
				
				theEventView = eventViews.select{ |eventView|
					eventView.state == \fadeIn
				
				}.at(0);
				
				if(theEventView.notNil){
					state = \fadeIn
				} {
					theEventView = 	eventViews.select{ |eventView|
						eventView.state == \fadeOut
					
					}.at(0);
					
					if(theEventView.notNil){
						state = \fadeOut
					} {
						theEventView = 	eventViews.select{ |eventView|
							eventView.state == \moving
						
						}.at(0);
						if(theEventView.notNil) {
							state = \moving;
							if(shiftDown.not) {
								if(theEventView.selected.not) {
									theEventView.selected = true;
									eventViews.do({ |eventView|
										if(eventView != theEventView) {
											eventView.selected = false
										}
									});
								} 
							} {
								theEventView.selected = theEventView.selected.not;
							};				
							if(altDown){
								isCopying = true;
								"going to copy";
							};					
						} {
							state = \selecting;
							selectionRect = Rect.fromPoints(mousePos,mousePos);
							if(clickCount == 2) {
							    if( scoreView.currentScore.isStopped ) {
                                    score.pos = mouseDownPos.x;
                                }
                            };
						}
					}
				}		
			}
						
		};
		
		//make sure there is only one theEventView being operated on
		if(theEventView.notNil) {
			eventViews.do{ |eventView|
				if(theEventView != eventView) {
					eventView.state = \nothing
				}			
			};
			if(clickCount == 2) {
                if(theEventView.event.isFolder){
                    fork{ 0.2.wait; {
                        scoreView.addtoScoreList(theEventView.event);
                    }.defer }
                } {
                    theEventView.event.gui;
                }
            }
		};

		//for making sure groups of events being moved are not sent off screen
		xLimit = this.selectedEventViews.collect({ |ev| ev.event.startTime }) !? _.minItem;
		yLimit = this.selectedEventViews.collect({ |ev| ev.event.track }) !? _.minItem;

        if( scoreView.currentScore.playState != \stopped) {
            if([\nothing, \selecting].includes(state).not) {
                state = \nothing;
            }
        };

		("Current state is "++state);
	}
	
	mouseXDelta{ |mousePos,scaledUserView|
		^mousePos.x - mouseDownPos.x
	}
	
	mouseYDelta{ |mousePos,scaledUserView|
		^mousePos.y - mouseDownPos.y
	}
	
	mouseMoveEvent{ |mousePos,unscaledMousePos,scaledUserView,snap,shiftDown,maxWidth|
		var deltaX, deltaY, scoreEvents, selEvents, newEvents, newEventViews;
		
		//check if movement exceeds threshold
		if((unscaledMousePos - mouseDownPos).x.abs > minimumMov) {
			//score will change store undo state
			if((mouseMoved == false) && [\nothing, \selecting].includes(state).not){

                scoreEditor.storeUndoState;
                scoreEditor.changed(\preparingToChangeScore);
			};
			mouseMoved = true;

			if( isCopying && copyed.not ) {
				//"copying Events".postln;
				
				selEvents = this.selectedEventViews;
				
				newEventViews = this.selectedEventViews.collect({ |ev,j|
					ev.duplicate(maxWidth).i_(eventViews.size + j).selected_(true).state_(\moving)
				});
				theEventView = newEventViews[0];
				
				eventViews.do{ |ev| ev.selected_(false).clearState };

                score.events = score.events ++ newEventViews.collect( _.event );
                eventViews = eventViews ++ newEventViews;

				//("scoreEvents "++score.events.size).postln;
				//("selected events"++this.selectedEventViews).postln;
				copyed = true;				
			};
		
			if([\nothing, \selecting].includes(state).not) {

				deltaX = this.mouseXDelta(mousePos,scaledUserView);
				deltaY = this.mouseYDelta(mousePos,scaledUserView).round( scaledUserView.gridSpacingV );
				if(state == \moving) {
					deltaX = deltaX.max(xLimit.neg);
					deltaY = deltaY.max(yLimit.neg);	
				};
				
				//if event is selected apply action all selected, otherwise apply action only to the event
				if(theEventView.selected) {
					
					this.selectedEventViews.do{ |eventView|
						("resizing "++eventView);
						eventView.mouseMoveEvent(deltaX,deltaY,state,snap,shiftDown)
					}
				} {
					theEventView.mouseMoveEvent(deltaX,deltaY,state,snap,shiftDown)
				}				

			} {
				
				"selecting now";
				//selecting
				selectionRect = Rect.fromPoints(mouseDownPos,mousePos);
			}
		}

		
	}
	
	mouseUpEvent{ |mousePos,unscaledMousePos,shiftDown,scaledUserView|
		var oldSelectedEvents;

		if(this.isResizingOrFades) {
		    //"resizing or fades".postln;
			if(mouseMoved.not) {
				eventViews.do{ |eventView|
					if(eventView.isResizingOrFades.not) {
						eventView.selected = false
					}{
						eventView.selected = true
					}	
				}
			}
				
		} {
			if((state == \moving)) {
				//"finished move".postln;
				if(mouseMoved.not){
				    //"mouse didn't move".postln;
				    state = \nothing;
					eventViews.do({ |eventView|
						if(shiftDown.not) {
							if(eventView != theEventView) {
								eventView.selected = false
							}
						}
					});
				};
				
			} {
	
				if(state == \selecting) {
					eventViews.do{ |eventView|
						eventView.checkSelectionStatus(selectionRect,shiftDown, scaledUserView.viewRect.width);
					};

				}
			}
		};
			
		/*if( UEventEditor.current.notNil && { this.selectedEventViews[0].notNil } ) {
			this.selectedEventViews[0].event.edit( parent: scoreEditor );
		};*/

        //score was changed, warn others !
        if( (mouseMoved == true) && [\nothing, \selecting].includes(state).not){
            score.changed(\something);
		};

		//go back to start state
		eventViews.do{ |eventView|
			eventView.clearState
		};
		mouseMoved = false;
		selectionRect = nil;
		state = \nothing;
		isCopying = false;
		copyed = false;

	}
	
	
}