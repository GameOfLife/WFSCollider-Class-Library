UScoreEditorGuiMouseEventsManager {
	classvar minimumMov = 3;
	var <scoreEditor;
	var <eventViews, <scoreEditorGUI, <>state = \nothing;
	var <mouseMoved = false, <mouseDownPos, <unscaledMouseDownPos;
	var <selectionRect, event;
	var <xLimit, <yLimit;
	var <isCopying = false, copyed = false;
	var <>mode = \all;
	var tranportPos = 0;
	var scoreEditorController;
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
	
	
	*new { |scoreEditor|
		^super.newCopyArgs(scoreEditor).init
	}

	init {
        this.makeEventViews;
	    scoreEditorController = SimpleController( scoreEditor );

		scoreEditorController.put(\score, {
		    //"rebuilding views".postln;
		    this.makeEventViews
		});
	}

	makeEventViews{
	    eventViews = scoreEditor.events.collect{ |event,i|
			event.makeView(i)
	    };
	}
	
	isResizing{
		^(state == \resizingFront) || (state == \resizingBack )
	}
	
	isResizingOrFades {
		^(state == \resizingFront) || (state == \resizingBack ) || (state == \fadeIn) || (state == \fadeOut )
	}
	
	selectedEventViews {	
		^this.eventViews.select{ |eventView|
			eventView.selected
		}
	}

	selectedEvents {
	    ^this.selectedEventViews.collect( _.event )
	}

	selectedEventsOrAll {
	    var v = this.selectedEventViews;
	    if(v.size > 0){
	        ^v.collect( _.event )
	    } {
	        ^scoreEditor.score.events
	    }
	}
		
	mouseDownEvent{ |mousePos,unscaledMousePos,shiftDown,altDown,scaledUserView|
		
		mouseDownPos = mousePos;
		unscaledMouseDownPos = unscaledMousePos;
		
		eventViews.do{ |eventView|
			eventView.mouseDownEvent(mousePos,scaledUserView,shiftDown,mode)
		};
		
		event = eventViews.select{ |eventView|
			eventView.state == \resizingFront
		}.at(0);
		
		if(event.notNil){
			state = \resizingFront
		} {
			event = eventViews.select{ |eventView|
				eventView.state == \resizingBack
			}.at(0);
			
			if(event.notNil){
				state = \resizingBack
			} {
				
				event = eventViews.select{ |eventView|
					eventView.state == \fadeIn
				
				}.at(0);
				
				if(event.notNil){
					state = \fadeIn
				} {
					event = 	eventViews.select{ |eventView|
						eventView.state == \fadeOut
					
					}.at(0);
					
					if(event.notNil){
						state = \fadeOut
					} {
						event = 	eventViews.select{ |eventView|
							eventView.state == \moving
						
						}.at(0);
						if(event.notNil) {
							state = \moving;
							if(shiftDown.not) {
								if(event.selected.not) {
									event.selected = true;
									eventViews.do({ |eventView|
										if(eventView != event) {
											eventView.selected = false
										}
									});
								} 
							} {
								event.selected = event.selected.not;
							};				
							if(altDown){
								isCopying = true;
								"going to copy";
							};					
						} {
							state = \selecting;
							selectionRect = Rect.fromPoints(mousePos,mousePos);
						}
					}
				}		
			}
						
		};
		
		//make sure there is only one event being operated on
		if(event.notNil) {
			eventViews.do{ |eventView|
				if(event != eventView) {
					eventView.state = \nothing
				}			
			};
		};
		
		//for making sure groups of events being moved are not sent off screen
		xLimit = this.selectedEventViews.collect({ |ev| ev.event.startTime }).minItem;
		yLimit = this.selectedEventViews.collect({ |ev| ev.event.track }).minItem;
		
		if([\nothing, \selecting].includes(state).not) {
			scoreEditor.storeUndoState;

		};
		
		("Current state is "++state);
	}
	
	mouseXDelta{ |mousePos,scaledUserView|
		^mousePos.x - mouseDownPos.x
	}
	
	mouseYDelta{ |mousePos,scaledUserView|
		^mousePos.y - mouseDownPos.y
	}
	
	mouseMoveEvent{ |mousePos,unscaledMousePos,scaledUserView,snap,shiftDown|
		var deltaX, deltaY, scoreEvents, selEvents, newEvents, newEventViews;
		
		//check if movement exceeds threshold
		if((unscaledMousePos - mouseDownPos).x.abs > minimumMov) {
			mouseMoved = true;

			if( isCopying && copyed.not ) {
				//"copying Events".postln;
				
				selEvents = this.selectedEventViews;
				
				newEventViews = this.selectedEventViews.collect({ |ev,j|
					ev.duplicate.i_(eventViews.size + j).selected_(true).state_(\moving)
				});
				event = newEventViews[0];
				
				eventViews.do{ |ev| ev.selected_(false).clearState };


				// NEEDS FIXING
                scoreEditor.score.events = scoreEditor.score.events ++ newEventViews.collect( _.event );
                eventViews = eventViews ++ newEventViews;

				//("scoreEvents "++scoreEditor.score.events.size).postln;

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
				if(event.selected) {
					
					this.selectedEventViews.do{ |eventView|
						("resizing "++eventView);
						eventView.mouseMoveEvent(deltaX,deltaY,state,snap,shiftDown)
					}
				} {
					event.mouseMoveEvent(deltaX,deltaY,state,snap,shiftDown)
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
							if(eventView != event) {
								eventView.selected = false
							}
						}
					});
				};
				
			} {
	
				if(state == \selecting) {
					eventViews.do{ |eventView|
						eventView.checkSelectionStatus(selectionRect,shiftDown);
					};
					if(mouseMoved.not) {
						scoreEditor.score.pos = mouseDownPos.x;
					};
				}
			}
		};
			
		/*if( UEventEditor.current.notNil && { this.selectedEventViews[0].notNil } ) {
			this.selectedEventViews[0].event.edit( parent: scoreEditor );
		};*/

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