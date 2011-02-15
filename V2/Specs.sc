ListSpec : Spec {
	var <list;
	var <>defaultIndex = 0; 
	var sortedList, indexMap;
	
	// handles only Symbols and Numbers, no repetitions
	
	*new { |list, defaultIndex = 0|
		^super.newCopyArgs( list ? [] ).init.defaultIndex_( defaultIndex );
	}
	
	init { 
		var tempList;
		tempList = list.collect({ |item| 
			if( item.isNumber.not ) { 
				item.asSymbol;
			} { 
				item;
			};
		});
		sortedList = tempList.copy.sort;
		indexMap = sortedList.collect({ |item| tempList.indexOfEqual( item ); });
	}
	
	default { ^this.at( defaultIndex ) }
	default_ { |value| defaultIndex = this.unmap( value ); }
	
	at { |index| ^list.at( index ); }
	put { |index, value| list.put( index, value ); this.init; }
	add { |value| list = list.add( value ); this.init; }
	remove { |value| list.remove( value ); this.init; }
	
	list_ { |newList| list = newList; this.init }
	
	constrain { |value|
		^list[ this.unmap( value ) ];
	}
	
	unmap { |value|
		var index;
		index = list.indexOf( value ); // first try direct (faster)
		if( index.notNil ) {
			^index;
		} {
			if( value.isNumber.not ) { value = value.asSymbol; };
			^indexMap[ sortedList.indexIn( value ) ] ? defaultIndex;
		};
	}
	
	map { |value|
		^list[ value.asInt ] ?? { list[ defaultIndex ] };
	}
	
}

BoolSpec : Spec {
	var <default = true;
	var <>trueLabel, <>falseLabel;
	
	*new { |default, trueLabel, falseLabel|
		^super.newCopyArgs( default ? true, trueLabel ? "true", falseLabel ? "false" );
	}
	
	map { |value|
		^value.booleanValue;
	}
	
	unmap { |value|
		^value.binaryValue;
	}
	
	constrain { |value|
		^value.booleanValue;
	}
	
	default_ { |value| 
		default = this.constrain( value );
	}
}

PointSpec : Spec {
	var <rect, <>step, >default, <>units; // constrains inside rect
	var clipRect;
	
	*new { |rect, step, default, units|
		^super.newCopyArgs( rect.asRect, (step ? 0).asPoint, default, units ? "" ).init;
	}
	
	init {
		clipRect = Rect.fromPoints( rect.leftTop, rect.rightBottom );
	}
	
	default { ^default ?? { clipRect.center.round( step ); } }
	
	minval { ^rect.leftTop }
	maxval { ^rect.rightBottom }
	
	minval_ { |value|
		var x,y;
		#x, y = value.asPoint.asArray;
		rect.left = x;
		rect.top = y;
		this.init;
	}
	
	maxval_ { |value|
		var x,y;
		#x, y = value.asPoint.asArray;
		rect.right = x;
		rect.top = y;
		this.init;
	}
	
	rect_ { |newRect| rect = newRect; this.init }
	
	clip { |value|
		^this.clip( clipRect.leftTop, clipRect.rightBottom );
	}
	
	constrain { |value|
		^value.asPoint.clip( clipRect.leftTop, clipRect.rightBottom ).round( step );
	}
	
	map { |value|
		^this.constrain( value.asPoint.linlin(0, 1, rect.leftTop, rect.rightBottom, \none ) );
	}
	
	unmap { |value|
		^this.constrain( value.asPoint ).linlin( rect.leftTop, rect.rightBottom, 0, 1, \none );
	}
}

RangeSpec : ControlSpec {
	var <>minRange, <>maxRange;
	var realDefault;
	
	// a range is an Array of two values [a,b], where:
	// a <= b, maxRange >= (b-a) >= minRange
	// the spec is a ControlSpec or possibly a ListSpec with numbers
	
	*new { |minval=0.0, maxval=1.0, minRange=0, maxRange = inf, warp='lin', step=0.0,
			 default, units|
		^super.new( minval, maxval, warp, step, default ? [minval,maxval], units )
			.minRange_( minRange ).maxRange_( maxRange )
	}
	
	*newFrom { arg similar; // can be ControlSpec too
		^this.new(similar.minval, similar.maxval, 
			similar.tryPerform( \minRange ) ? 0,
			similar.tryPerform( \maxRange ) ? inf,
			similar.warp.asSpecifier, 
			similar.step, similar.default, similar.units)
	}
	
	default_ { |range| realDefault = default = this.constrain( range ); }
	default { ^realDefault ?? 
		{ realDefault = this.constrain( default ? [minval, maxval] ); } } // in case of a bad default
	
	storeArgs { ^[minval, maxval, minRange, maxRange, warp.asSpecifier, step, this.default, units] }
	
	constrain { arg value;
		var array;
		array = value.asArray.sort;
		if( array.size != 2 ) { array.extend( 2, array.last ); };
		array = array.collect({ |item| item.asFloat.clip( clipLo, clipHi ); });
		case { (array[1] - array[0]) < minRange } { 
			//"clipped minRange".postln;
			array = array.mean + ( minRange * [-0.5,0.5] );
			case { array[0] < clipLo } {
				array = array + (clipLo-array[0]);
			} {  array[1] > clipHi } {
				array = array + (clipHi-array[1]);
			}; 
		} { (array[1] - array[0]) > maxRange } {
			//"clipped maxRange".postln;
			array = array.mean + ( maxRange * [-0.5,0.5] );
			case { array[0] < clipLo } {
				array = array + (clipLo-array[0]);
			} {  array[1] > clipHi } {
				array = array + (clipHi-array[1]);
			}; 
		};
		^array.round(step); // step may mess up the min/maxrange
	}
	
	map { arg value;
		// maps a value from [0..1] to spec range
		^this.constrain( warp.map(value) );
	}
	
	unmap { arg value;
		// maps a value from spec range to [0..1]
		^warp.unmap( this.constrain(value) );
	}
	
	asRangeSpec { ^this }
	asControlSpec { ^ControlSpec.newFrom( this ).default_( this.default[0] ); }

}

BufferSpec : Spec {
	
	var <>numChannels;
	var <numFrames;
	var <>isWavetable = false;

	
	*new { |numChannels = 1, numFrames, isWavetable = false|
		^super.newCopyArgs( numChannels, numFrames, isWavetable ).init;
	}
	
	init {
		if( numFrames.isNumber ) { numFrames = [numFrames,numFrames].asSpec }; // single value
		if( numFrames.isNil ) { numFrames = [0,inf].asSpec }; // endless
		numFrames = numFrames.asSpec;
	}
	
	numFrames_ { |inNumFrames| numFrames = inNumFrames; this.init; }
	
	newBuffer { |server, inNumFrames, bufnum|
		if( inNumFrames.isNil ) { inNumFrames = numFrames.default };
		^Buffer.new( server, numFrames.constrain( inNumFrames ), numChannels, bufnum );
	}
	
	allocBuffer { |server, inNumFrames, completionMessage, bufnum|		if( inNumFrames.isNil ) { inNumFrames = numFrames.default };
		^Buffer.alloc( server, numFrames.constrain( inNumFrames ), numChannels, 
			completionMessage, bufnum );
	}
	
	makeBuffer { |server, inNumFrames, completionMessage, bufnum|
		^this.allocBuffer( server, inNumFrames, completionMessage, bufnum );
	}
	
}

SoundFileSpec : BufferSpec {
	
	var <>sampleRates = \any; // 'any', value or array
	var <>diskIn = false;
	
	
	
}


+ ControlSpec { 
	asRangeSpec { ^RangeSpec.newFrom( this ) }
	asControlSpec { ^this }
}

+ Nil {
	asRangeSpec { ^RangeSpec.new }
	asControlSpec { ^this.asSpec; }
}

+ Symbol {
	asRangeSpec { ^this.asSpec.asRangeSpec }
	asControlSpec { ^this.asSpec; }
}

+ Array {
	asRangeSpec { ^RangeSpec.newFrom( *this ) }
	asControlSpec { ^this.asSpec; }
}