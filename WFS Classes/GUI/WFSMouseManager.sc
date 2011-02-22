WFSMouseEventsManager {
	classvar minimumMov = 3;
	var <eventViews, <wfsScoreEditor, <>state = \nothing;
	var <mouseMoved = false, <mouseDownPos, <unscaledMouseDownPos;
	var <selectionRect, event;
	var <xLimit, <yLimit;
	var <isCopying = false, copyed = false;
	var <>mode = \all;
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
	
	
	*new { |eventViews,wfsScoreEditor|
		^super.newCopyArgs(eventViews,wfsScoreEditor)
	}
	
	isResizing{
		^(state == \resizingFront) || (state == \resizingBack )
	}
	
	isResizingOrFades {
		^(state == \resizingFront) || (state == \resizingBack ) || (state == \fadeIn) || (state == \fadeOut )
	}
	
	selectedEvents {	
		^this.eventViews.select{ |eventView|
			eventView.selected
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
				
				event = 	eventViews.select{ |eventView|
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
		xLimit = this.selectedEvents.collect({ |ev| ev.event.startTime }).minItem;
		yLimit = this.selectedEvents.collect({ |ev| ev.event.track }).minItem;
		
		if([\nothing, \selecting].includes(state).not) {
			wfsScoreEditor.storeUndoState
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
			"mouse moved";
			if( isCopying && copyed.not ) {
				"copying Events";
				
				
				selEvents = this.selectedEvents;
				
				newEventViews = this.selectedEvents.collect({ |ev,j| 
					ev.duplicate.i_(eventViews.size + j).selected_(true).state_(\moving)
				});
				event = newEventViews[0];
				
				eventViews.do{ |ev| ev.selected_(false).clearState };
				
				wfsScoreEditor.wfsEventViews = eventViews = eventViews ++ newEventViews;

				wfsScoreEditor.score.events = wfsScoreEditor.score.events++ newEventViews.collect(_.event);
				("scoreEvents "++wfsScoreEditor.score.events.size);
				
			
				("selected events"++this.selectedEvents);
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
					
					this.selectedEvents.do{ |eventView|
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
			if(mouseMoved.not) {
				eventViews.do { |eventView|
					if(eventView.isResizingOrFades.not) {
						eventView.selected = false
					}{
						eventView.selected = true
					}	
				}
			}
				
		} {
			if((state == \moving)) {
				"finished move";
				if(mouseMoved.not){
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
						WFSTransport.pos_(mouseDownPos.x);		
					};
				}
			}
		};
			
		if( WFSEventEditor.current.notNil && { this.selectedEvents[0].notNil } ) {
			this.selectedEvents[0].event.edit( parent: wfsScoreEditor );
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