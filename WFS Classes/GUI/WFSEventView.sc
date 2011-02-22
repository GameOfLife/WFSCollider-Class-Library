
WFSEventView {
	
	var <>event, <>i, rect, <>selected = false, <>state = \nothing;
	var <>originalStartTime, <>originalEndTime, <>originalFades, <>originalTrack;
	//state is \nothing, \moving, \resizingFront, \resizingBack, \selecting, \fadeIn, \fadeOut;

	*new{ |event,i|
		^super.newCopyArgs(event,i).createRect
	}
	
	//notice variable i will also be the same...
	duplicate{ 
		^WFSEventView(event.duplicate,i)
		.originalStartTime_(originalStartTime)
		.originalEndTime_(originalEndTime)
		.originalFades_(originalFades)
		.originalTrack_(originalTrack)
	}
	
	isResizing{
		^(state == \resizingFront) || (state == \resizingBack )
	}
	
	isResizingOrFades {
		^(state == \resizingFront) || (state == \resizingBack ) || (state == \fadeIn) || (state == \fadeOut )
	}
	
	createRect {
		rect = Rect( event.startTime, event.track, event.dur, 1 );
	}

	getTypeColor { 
		
		var color;
		color = if( event.isFolder )
			{ Color.white;  }
			{ 	( ( 	'buf': Color.blue, 
					'disk': Color.magenta,
					'blip': Color.red  )
					[ event.wfsSynth.audioType ] ? Color.gray ).blend(
				 ( ( 	'linear': Color(0.35,0.95,0.35), 
						'cubic': Color.cyan(0.75),
						'static': Color.green(0.75),
						'plane': Color.yellow(0.75) )
					[ event.wfsSynth.intType ] ? Color.gray ), 0.5 )
			};

		^color;
	}

	getName { 
		var audioType, outString;
		audioType = event.wfsSynth.audioType;
		outString = (case { audioType == \folder }
			{ i.asString ++ ": folder "++ event.wfsSynth.name++" (" ++ event.wfsSynth.events.size ++ " events)"  }
			{ [\buf, \disk].includes( audioType ) }
			{ i.asString ++ ": " ++ event.wfsSynth.filePath.basename; }
			{ audioType === \blip }
			{ i.asString ++ ": testevent (blip)"  }
			{ true }
			{ i.asString } );
		if( event.muted ) 
			{ ^outString ++ " (muted)" }
			{ ^outString };
	}
	
	mouseDownAll{ |mousePos,scaledUserView,shiftDown|
		var px5Scaled =  scaledUserView.doReverseScale(Point(5,0)).x; 
		var px10Scaled = scaledUserView.doReverseScale(Point(10,0)).x;
		var resizeFrontDetectArea,resizeBackDetectArea,fadeInDetectArea,fadeOutDetectArea, fadeInB10, fadeOutB10, fadeAreaHeight;
		
		this.createRect;
		
		fadeAreaHeight = (rect.height*0.3);
		
		if(event.isFolder.not) {
			
			if(rect.containsPoint(mousePos)) {
		
				fadeInB10 = event.fadeInTime > px10Scaled;
				
				if(fadeInB10) {
					resizeFrontDetectArea = rect.copy.width_(px5Scaled);
				} {
					resizeFrontDetectArea = rect.copy.width_(px5Scaled).height_(rect.height-fadeAreaHeight).top_(rect.top+fadeAreaHeight);
				};
				//resize front
				if(resizeFrontDetectArea.containsPoint( mousePos )) {
					
					state = \resizingFront;
					originalStartTime = event.startTime;
					event.wfsSynth.checkSoundFile;
					
				}{
					fadeOutB10 = event.fadeOutTime > px10Scaled;
					if(fadeOutB10) {
						resizeBackDetectArea = rect.copy.width_(px5Scaled).left_((rect.left + rect.width - px5Scaled))
					} {
						resizeBackDetectArea = rect.copy.width_(px5Scaled).left_((rect.left + rect.width - px5Scaled))
						.height_(rect.height-fadeAreaHeight).top_(rect.top+fadeAreaHeight)
					};
					//resize back	
					if(resizeBackDetectArea.containsPoint( mousePos )) {
						
						state = \resizingBack;
						originalEndTime = event.endTime;
						event.wfsSynth.checkSoundFile;
						
					} {
						if(event.fadeInTime > px10Scaled) {
							fadeInDetectArea = rect.copy.width_(px10Scaled).left_(rect.left + event.fadeInTime - px5Scaled);
						} {
							fadeInDetectArea = rect.copy.width_(px5Scaled).height_(fadeAreaHeight);
						};
						//fade in
						if( fadeInDetectArea.contains( mousePos ) ) {
							state = \fadeIn;
							originalFades = event.wfsSynth.fadeTimes.copy;
							
						} {
							if(event.fadeOutTime > px10Scaled) {						fadeOutDetectArea = rect.copy.width_(px10Scaled).left_((rect.right - event.fadeOutTime - px5Scaled)) 
							} {
								fadeOutDetectArea = rect.copy.width_(px5Scaled).left_((rect.right - px5Scaled)).height_(fadeAreaHeight);	
							};
							//fade out
							if(fadeOutDetectArea.contains( mousePos ) ) {
								"fade out";
								state = \fadeOut;
							originalFades = event.wfsSynth.fadeTimes.copy;
								
							} {	//moving
								state = \moving;
								originalTrack = event.track;
								originalStartTime = event.startTime;
								originalEndTime = event.endTime;
		
							
							}
						}				
					}
				}
			} {
				if(selected) {
					originalStartTime = event.startTime;
					originalEndTime = event.endTime;
					originalFades = event.wfsSynth.fadeTimes.copy;
					originalTrack = event.track;
					event.wfsSynth.checkSoundFile;
				}
			}
		} {
			if(rect.containsPoint(mousePos)) {
				state = \moving;
				originalTrack = event.track;
				originalStartTime = event.startTime;
				originalEndTime = event.endTime;

			} {
				if(selected) {
					originalStartTime = event.startTime;
					originalEndTime = event.endTime;
					originalTrack = event.track;
				}
			}
		}
	
	}
	
	mouseDownMove{ |mousePos,scaledUserView,shiftDown|
		var px5Scaled =  scaledUserView.doReverseScale(Point(5,0)).x; 
		var px10Scaled = scaledUserView.doReverseScale(Point(10,0)).x;
		var resizeFrontDetectArea,resizeBackDetectArea,fadeInDetectArea,fadeOutDetectArea, fadeInB10, fadeOutB10, fadeAreaHeight;
		
		this.createRect;
			
		if(event.isFolder.not) {		
			//moving
			if(rect.containsPoint(mousePos)) {
				state = \moving;
				originalTrack = event.track;
				originalStartTime = event.startTime;
				originalEndTime = event.endTime;

			} {
				if(selected) {
					originalStartTime = event.startTime;
					originalEndTime = event.endTime;
					originalTrack = event.track;
					event.wfsSynth.checkSoundFile;
				}
			}
						
		} {
			if(rect.containsPoint(mousePos)) {
				state = \moving;
				originalTrack = event.track;
				originalStartTime = event.startTime;
				originalEndTime = event.endTime;

			} {
				if(selected) {
					originalStartTime = event.startTime;
					originalEndTime = event.endTime;
					originalTrack = event.track;
				}
			}
		}
	
	}
	
	mouseDownResize{ |mousePos,scaledUserView,shiftDown|
		var px5Scaled =  scaledUserView.doReverseScale(Point(5,0)).x; 
		var px10Scaled = scaledUserView.doReverseScale(Point(10,0)).x;
		var resizeFrontDetectArea,resizeBackDetectArea,fadeInDetectArea,fadeOutDetectArea, fadeInB10, fadeOutB10, fadeAreaHeight;
		
		this.createRect;
				
		if(event.isFolder.not) {
			
			if(rect.containsPoint(mousePos)) {
		
				fadeInB10 = event.fadeInTime > px10Scaled;
				
				if(fadeInB10) {
					resizeFrontDetectArea = rect.copy.width_(px5Scaled);
				} {
					resizeFrontDetectArea = rect.copy.width_(px5Scaled).height_(rect.height-fadeAreaHeight).top_(rect.top+fadeAreaHeight);
				};
				//resize front
				if(resizeFrontDetectArea.containsPoint( mousePos )) {
					
					state = \resizingFront;
					originalStartTime = event.startTime;
					event.wfsSynth.checkSoundFile;
					
				}{
					fadeOutB10 = event.fadeOutTime > px10Scaled;
					if(fadeOutB10) {
						resizeBackDetectArea = rect.copy.width_(px5Scaled).left_((rect.left + rect.width - px5Scaled))
					} {
						resizeBackDetectArea = rect.copy.width_(px5Scaled).left_((rect.left + rect.width - px5Scaled))
						.height_(rect.height-fadeAreaHeight).top_(rect.top+fadeAreaHeight)
					};
					//resize back	
					if(resizeBackDetectArea.containsPoint( mousePos )) {
						
						state = \resizingBack;
						originalEndTime = event.endTime;
						event.wfsSynth.checkSoundFile;
						
					}
				}
			} {					
				if(selected) {
					originalStartTime = event.startTime;
					originalEndTime = event.endTime;
					event.wfsSynth.checkSoundFile;
				}				
			}
		}	
	}
	
	mouseDownFades{ |mousePos,scaledUserView,shiftDown|
		var px5Scaled =  scaledUserView.doReverseScale(Point(5,0)).x; 
		var px10Scaled = scaledUserView.doReverseScale(Point(10,0)).x;
		var resizeFrontDetectArea,resizeBackDetectArea,fadeInDetectArea,fadeOutDetectArea, fadeInB10, fadeOutB10;
		
		this.createRect;
							
		if(event.isFolder.not) {
			
			if(rect.containsPoint(mousePos)) {		
			
				fadeInDetectArea = rect.copy.width_(px10Scaled).left_(rect.left + event.fadeInTime - px5Scaled);
			
				//fade in
				if( fadeInDetectArea.contains( mousePos ) ) {
					state = \fadeIn;
					originalFades = event.wfsSynth.fadeTimes.copy;
					
				} {
					fadeOutDetectArea = rect.copy.width_(px10Scaled).left_((rect.right - event.fadeOutTime - px5Scaled));
					
					//fade out
					if(fadeOutDetectArea.contains( mousePos ) ) {
						"fade out";
						state = \fadeOut;
					originalFades = event.wfsSynth.fadeTimes.copy;
						
					} 
				}
			} {	
				if(selected) {
					originalFades = event.wfsSynth.fadeTimes.copy;
					event.wfsSynth.checkSoundFile;
				}
			
			}				
			
		}	
	}

	mouseDownEvent{ |mousePos,scaledUserView,shiftDown,mode|
		
		switch(mode)
		{\all}{ this.mouseDownAll(mousePos,scaledUserView,shiftDown) }
		{\move}{ this.mouseDownMove(mousePos,scaledUserView,shiftDown) }
		{\resize}{ this.mouseDownResize(mousePos,scaledUserView,shiftDown) }
		{\fades}{ this.mouseDownFades(mousePos,scaledUserView,shiftDown) }
		
	}
	
	mouseMoveEvent{ |deltaTime, deltaTrack, overallState, snap, moveVert|
		
		if(event.isFolder.not) {
			switch(overallState)
			{\resizingFront}
			{
				event.trimStart( originalStartTime + deltaTime )
			}
			{\resizingBack}
			{
				"resizing back";
				event.trimEnd( originalEndTime + deltaTime )
			}
			{\moving}
			{
				if( moveVert.not ) {
					event.startTime = (originalStartTime + deltaTime).round(snap)
				};
				event.track = originalTrack + deltaTrack;
			}
			{\fadeIn}
			{
				event.wfsSynth.fadeInTime = originalFades[0] + deltaTime;
				event.wfsSynth.clipFadeIn;
			}
			{\fadeOut}
			{
				event.wfsSynth.fadeOutTime = originalFades[1] - deltaTime;
				event.wfsSynth.clipFadeOut;
			}
		} {
			if(overallState == \moving) {
				if( moveVert.not ) {
					event.startTime = (originalStartTime + deltaTime).round(snap)
				};
				event.track = originalTrack + deltaTrack;
			}
		}		
	}

	clearState{
		state = \nothing;		
	}

	checkSelectionStatus { |selectionRect,shiftDown|
		this.createRect;
		if(selectionRect.intersects(rect)) {
			selected = true
		} {
			if(shiftDown.not) {
				selected = false
			}
		}
	}
	
	drawShape { |rectToDraw|
		var radius = 5;

		radius = radius.min( rectToDraw.height*0.5 );
		Pen.moveTo(rectToDraw.rightTop - Point(rectToDraw.width*0.5,0));
		Pen.arcTo(rectToDraw.rightTop,rectToDraw.rightBottom,radius);
		Pen.lineTo(rectToDraw.rightBottom);
		Pen.lineTo(rectToDraw.leftBottom);
		Pen.arcTo(rectToDraw.leftTop,rectToDraw.leftTop + Point(rectToDraw.width*0.5,0),radius);
		Pen.lineTo(rectToDraw.leftTop +  Point(rectToDraw.width*0.5,0));
		
	}	

	draw { |scaledUserView|
		var textrect;
		var muted = event.muted;
		var lineAlpha =  if( muted ) { 0.5  } { 1.0  };
		var selectedAlpha = if( selected ) { 0.8 } { 1 };
		var scaledRect, innerRect, clipRect;
		var px10Scaled = scaledUserView.doReverseScale(Point(10,0)).x;
		var px5Scaled =  scaledUserView.doReverseScale(Point(5,0)).x;
		
		this.createRect;
		
		scaledRect = scaledUserView.translateScale(rect);
		innerRect = scaledRect.insetBy(0.5,0.5);
		clipRect = scaledUserView.view.drawBounds.moveTo(0,0).insetBy(2,2);
		
		//selected outline		
		if( selected ) {
			Pen.width = 2;
			Pen.color = Color.grey(0.2);
			this.drawShape(scaledRect);
			Pen.stroke;
		};		
		
		//fill inside
		Pen.color = this.getTypeColor.alpha_(
			lineAlpha * 0.4);	
		this.drawShape(innerRect);	
		Pen.fill;
		 
		//draw fades
		if(((event.wfsSynth.class == WFSScore) or: { event.wfsSynth.fadeTimes.isNil }).not) {
			Pen.use({
				var fadeinScaled, fadeoutScaled, fades;
				
				fades = event.wfsSynth.fadeTimes;	
							
				this.drawShape(innerRect);
				Pen.clip;
				
				Pen.color = this.getTypeColor.alpha_(lineAlpha * 0.8);
				
				fadeinScaled = scaledUserView.doScale(Point(fades[0],0));
				fadeoutScaled = scaledUserView.doScale(Point(fades[1],0));						
				Pen.moveTo(innerRect.leftBottom);
				Pen.lineTo(innerRect.rightBottom);
				Pen.lineTo(innerRect.rightBottom - fadeoutScaled - Point(0,innerRect.height) );
				Pen.lineTo(innerRect.leftBottom + fadeinScaled + Point(0,innerRect.height.neg));
				Pen.moveTo(innerRect.leftBottom);
				
				Pen.fill;
				
				//fade lines
				Pen.width = 1;
				Pen.color = Color.grey(0.3).alpha_(0.5);
				Pen.moveTo(innerRect.rightBottom - fadeoutScaled - Point(0,innerRect.height));
				Pen.lineTo(innerRect.rightBottom - fadeoutScaled);
				Pen.stroke;
				Pen.moveTo(innerRect.leftBottom + fadeinScaled + Point(0,innerRect.height.neg));
				Pen.lineTo(innerRect.leftBottom + fadeinScaled);
				Pen.stroke;
				
			});

		};
		
				
		Pen.color = Color.black.alpha_( lineAlpha  );
				
		if( scaledRect.height > 4 ) {

			textrect = scaledRect.sect( scaledUserView.view.drawBounds.moveTo(0,0).insetBy(-3,0) ); 
			Pen.use({		
				Pen.addRect( textrect ).clip;
				Pen.stringLeftJustIn( 
					" " ++ this.getName, 
					textrect );
			});
			
			//show region bounds
			if(event.isFolder.not) {
				if(scaledRect.height > 35){
				Pen.use({
					var rect = scaledRect.copy.width_(30).height_(scaledRect.height/2).moveTo(scaledRect.left,scaledRect.top+(scaledRect.height/2));
					var seconds = event.wfsSynth.startFrame/44100;				var string = SMPTE.global.initSeconds(seconds,25).format("mm:ss");
					Pen.addRect(rect).clip;
					Pen.stringLeftJustIn(string,rect);
				});
				Pen.use({
					var rect = scaledRect.copy.width_(30).height_(scaledRect.height/2).moveTo(scaledRect.right-30,scaledRect.top+(scaledRect.height/2));
					var seconds = (event.wfsSynth.startFrame/44100)+event.dur;
					var string = SMPTE.global.initSeconds(seconds,25).format("mm:ss");
					Pen.addRect(rect).clip;
					Pen.stringRightJustIn(string,rect);
				});
				}
			}
		};
		
		/*
		// DEBUG resize area
		Pen.color = Color.red;
		Pen.addRect(scaledRect.copy.width_(5).height_(scaledRect.height-6).top_(scaledRect.top+6));
		Pen.fill;
		Pen.color = Color.red;
		Pen.addRect(scaledRect.copy.width_(5).left_(scaledRect.left+scaledRect.width-5).height_(scaledRect.height-6).top_(scaledRect.top+6));
		Pen.fill;
		*/

	}
	
}
