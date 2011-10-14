
UEventView {

	var <>event, <>i, rect, <>selected = false, <>state = \nothing;
	var <>originalStartTime, <>originalEndTime, <>originalFades, <>originalTrack;
	var <px5Scaled, <px10Scaled;
	//state is \nothing, \moving, \resizingFront, \resizingBack, \selecting, \fadeIn, \fadeOut;

	*new{ |event,i,maxWidth|
		^super.newCopyArgs(event,i).createRect(maxWidth)
	}

	//notice variable i will also be the same...
	duplicate{ |maxWidth|
		^this.class.new(event.duplicate,i, maxWidth)
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

	createRect { |maxWidth|
	    var dur = event.dur;
	    dur = if( dur == inf){maxWidth-event.startTime}{event.dur};
		rect = Rect( event.startTime, event.track, dur, 1 );
	}

	getTypeColor { }

	getName { }

	getFullName{
	    ^if( event.muted )
			{ this.getName ++ " (muted)" }
			{ this.getName };
	}

	mouseDownEvent{	}

	mouseMoveEvent{ }

	clearState{
		state = \nothing;
	}

	checkSelectionStatus { |selectionRect,shiftDown, maxWidth|
		this.createRect(maxWidth);
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

	draw { }
}