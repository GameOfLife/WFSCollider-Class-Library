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
		^super.newCopyArgs( default ? true, trueLabel, falseLabel );
	}
	
	*testObject { |obj|
		^[ True, False ].includes( obj.class );
	}
	
	*newFromObject { |obj|
		^this.new( obj );
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
	
	classvar <>defaultMode = \point;
	
	var <rect, <>step, >default, <>units, <>mode; // constrains inside rect
	var clipRect;
	
	// mode can be \point, \polar, \deg_cw, \deg_ccw
	// only for gui; output will always be Point

	*new { |rect, step, default, units, mode|
		^super.newCopyArgs( rect, (step ? 0).asPoint, default, units ? "", mode ).init;
	}
	
	*testObject { |obj|
		^obj.class == Point;
	}
	
	*newFromObject { |obj|
		var cspecs;
		cspecs = obj.asArray.collect({ |item| ControlSpec.newFromObject( item ) });
		^this.new( Rect.fromPoints( 
			(cspecs[0].minval)@(cspecs[1].minval), 
			(cspecs[0].maxval)@(cspecs[1].maxval) ),
			(cspecs[0].step)@(cspecs[1].step),
			obj );
	}
	
	init {
		// number becomes radius
		if( rect.isNumber ) { rect = Rect.aboutPoint( 0@0, rect, rect ); };
		rect = rect.asRect;
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

PolarSpec : Spec {
	
	var <>maxRadius, <step, >default, <>units; // constrains inside rect
	var clipRect;
	
	*new { |maxRadius, step, default, units| 			
		^super.newCopyArgs( maxRadius, (step ? 0), default, units ? "" ).init;
	}
	
	*testObject { |obj|
		^obj.class == Polar;
	}
	
	*newFromObject { |obj|
		var cspec;
		cspec = ControlSpec.newFromObject( obj.rho );
		^this.new( cspec.maxval, cspec.step, obj );
	}
	
	init {
		if( step.class != Polar ) {
			step = Polar( step ? 0, 0 );
		};
	}
	
	default { ^default ?? { clipRect.center.round( step ); } }
	
	step_ { |inStep| step = this.makePolar( inStep ) }
	
	makePolar { |value|
		if( value.class != Polar ) {
			if( value.isArray ) {
				^Polar( *value );
			} {
				^value.asPoint.asPolar;
			};
		} {
			^value.copy;
		};
	}
	
	clipRadius { |value|
		value = this.makePolar( value );
		if( maxRadius.notNil ) {
			value.rho = value.rho.clip2( maxRadius ); // can be negative too
		};
		^value;
	}
	
	roundToStep { |value|
		value = this.makePolar( value );
		value.rho = value.rho.round( step.rho );
		value.theta = value.theta.round( step.theta );
		^value;
	}
	
	scaleRho { |value, amt = 1|
		value = this.makePolar( value );
		value.rho = value.rho * amt;
		^value;
	}
	
	constrain { |value|
		value = this.clipRadius( value );
		^this.roundToStep( value );
	}
	
	map { |value|
		^this.constrain( this.scaleRho( value, maxRadius ? 1 ) );
	}
	
	unmap { |value|
		^this.scaleRho( this.constrain( value ), 1/(maxRadius ? 1));
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
	
	*testObject { |obj|
		^obj.isArray && { (obj.size == 2) && { obj.every(_.isNumber) } };
	}
	
	*newFromObject { |obj|
		var cspecs;
		cspecs = obj.collect({ |item| ControlSpec.newFromObject( item ) });
		^this.new( 
			cspecs.collect(_.minval).minItem, 
			cspecs.collect(_.maxval).maxItem, 
			0, inf, \lin, 
			cspecs.collect(_.step).minItem, 
			obj
			);
	}
	
	
	default_ { |range| realDefault = default = this.constrain( range ); }
	default { ^realDefault ?? 
		{ realDefault = this.constrain( default ? [minval, maxval] ); } } // in case of a bad default
	
	storeArgs { ^[minval, maxval, minRange, maxRange, warp.asSpecifier, step, this.default, units] }
	
	constrain { arg value;
		var array;
		array = value.asArray.copy.sort;
		if( array.size != 2 ) { array = array.extend( 2, array.last ); };
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
	
	var <>numChannels = 1; // fixed number of channels
	var <numFrames;

	
	*new { |numChannels = 1, numFrames|
		^super.newCopyArgs( numChannels, numFrames ).init;
	}
	
	*testObject { |obj|
		^obj.class == RichBuffer; // change to bufferholder later
	}
	
	*newFromObject { |obj|
		^this.new( obj.numChannels );
	}
	
	init {
		if( numFrames.isNumber ) { numFrames = [numFrames,numFrames].asSpec }; // single value
		if( numFrames.isNil ) { numFrames = [0,inf,\lin,1,44100].asSpec }; // endless
		numFrames = numFrames.asSpec;
	}
	
	constrain { |value|
		if( value.class == RichBuffer ) {
			value.numFrames = numFrames.constrain( value.numFrames );
			value.numChannels = numChannels;
			^value;
		} {
			^RichBuffer( numChannels, numFrames.default );
		};
	}
	
}

BufSndFileSpec : BufferSpec {
	
	*testObject { |obj|
		^obj.isKindOf( BufSndFile );
	}
	
	constrain { |value|
		case { value.class == DiskSndFile } {
			value = value.asBufSndFile;
		} { value.class != BufSndFile } {
			value = this.default;
		};
		
		if( value.numChannels != numChannels ) {
			if( value.useChannels.size != numChannels ) {
				value.useChannels = (..numChannels-1).wrap( 0, value.numChannels );
			};
		};
		^value;
	}
	
	default { 
		^BufSndFile.newBasic("sounds/a11wlk01-44_1.aiff", 107520, 1, 44100, 0, nil, 1, false)
	}
	
	*newFromObject { |obj|
		^this.new( obj.numChannels );
	}
	
}

DiskSndFileSpec : BufferSpec {
	
	*testObject { |obj|
		^obj.isKindOf( DiskSndFile );
	}
	
	constrain { |value|
		case { value.class == BufSndFile } {
			value = value.asDiskSndFile;
		} { value.class != DiskSndFile } {
			value = this.default;
		};
		if( value.numChannels != numChannels ) {
			"DiskSndFileSpec - soundfile '%' should have % channels but has %.\nIt might not playback"
				.format( value.path.basename, numChannels, value.numChannels );
		};
		^value;
	}
	
	default { 
		^DiskSndFile.newBasic("sounds/a11wlk01-44_1.aiff", 107520, 1, 44100, 0, nil, 1, false)
	}
	
	*newFromObject { |obj|
		^this.new( obj.numChannels );
	}
	
}

MultiSpec : Spec {
	
	// an ordered and named collection of specs, with the option to re-map to another spec
	
	var <names, <specs, <>defaultSpecIndex = 0;
	var <>toSpec;
	
	*new { |...specNamePairs|
		specNamePairs = specNamePairs.clump(2).flop;
		^super.newCopyArgs( specNamePairs[0], specNamePairs[1] ).init;
	}
	
	init {
		names = names.asCollection.collect(_.asSymbol);
		specs = specs.asCollection;
		specs = names.collect({ |item, i| specs[i].asSpec });
	}
	
	findSpecForName { |name| // name or index
		name = name ? defaultSpecIndex;
		if( name.isNumber.not ) { name = names.indexOf( name.asSymbol ) ? defaultSpecIndex };
		^specs[ name ];
	}
	
	default { |name| // each spec has it's own default
		^this.findSpecForName(name).default;
	}
	
	defaultName { ^names[ defaultSpecIndex ] }
	defaultName_ { |name| defaultSpecIndex = names.indexOf( name.asSymbol ) ? defaultSpecIndex }
	
	defaultSpec { ^specs[ defaultSpecIndex ] }
	
	constrain { |value, name|
		^this.findSpecForName(name).constrain( value );
	}
	
	map { |value, name|
		if( toSpec.notNil ) { value = toSpec.asSpec.unmap( value ) };
		^this.findSpecForName(name).map( value );
	}
	
	unmap { |value, name|
		if( toSpec.notNil ) { value = toSpec.asSpec.map( value ) };
		^this.findSpecForName(name).unmap( value );
	}
	
	mapToDefault { |value, from|
		if( from.isNil ) { value = this.unmap( value, from ); };
		^this.map( value, defaultSpecIndex );
	}
	
	unmapFromDefault { |value, to|
		value = this.unmap( value, defaultSpecIndex );
		if( to.isNil ) { 
			^this.map( value, to ); 
		} {
			^value
		};	
	}
	
	mapFromTo { |value, from, to|
		^this.map( this.unmap( value, from ), to );
	}
	
	unmapFromTo { |value, from, to|
		^this.mapFromTo( value, to, from );
	}
}

EQSpec : Spec {
    var <specs;// array of size 5, each element an array with 3 specs for freq, level, rq

    *new{
        ^super.new.init
    }

    init{
        var defaultFreqs = [100,250,1000,3500,6000];
        specs = defaultFreqs.collect{ |f|
            [ControlSpec(20, 20000, \exp, 0, f, units: " Hz"), \unipolar.asSpec, [0.01,10,\exp,0.0,1.0].asSpec]
        }

    }

    constrain{ |eqArg|
        ^EQArg([eqArg.values,specs].flop.collect{ |array|
            array.flop.collect{ |array|
                array[1].constrain(array[0])
            }.flop
        }.flop)
    }

    defaultArray {
        ^specs.collect{ |specs|
            specs.collect(_.default)
        }
    }

    default{
        ^EQArg(this.defaultArray)
    }

}

IntegerSpec : Spec {

	var <default = 0;
	var <step = 1;

    *new{ |default = 0|
        ^super.new.default_(default)
    }

    *testObject { |obj|
    		^obj.class == Integer;
    }

	constrain { |value|
		^value.asInteger;
	}

	default_ { |value|
		default = this.constrain( value );
	}
}

PositiveIntegerSpec : IntegerSpec {

	constrain { |value|
		^value.asInteger.max(0);
	}

}

PositiveRealSpec : Spec {

	var <default = 0;

    *new{ |default = 0|
        ^super.new.default_(default)
    }

	constrain { |value|
		^value.max(0);
	}

	default_ { |value|
		default = this.constrain( value );
	}
}

+ Spec {
	*testObject { ^false }
	
	*forObject { |obj|
		var specClass;
		specClass = [ ControlSpec, RangeSpec, BoolSpec, PointSpec, PolarSpec, 
				BufSndFileSpec, DiskSndFileSpec ]
			.detect({ |item| item.testObject( obj ) });
		if( specClass.notNil ) {
			^specClass.newFromObject( obj );
		} {
			^nil;
		};
	}
	
	*newFromObject { ^this.new }
}


+ ControlSpec { 
	asRangeSpec { ^RangeSpec.newFrom( this ) }
	asControlSpec { ^this }
	
	*testObject { |obj| ^obj.isNumber }
	
	*newFromObject { |obj| // float or int
		var range;
		
		if( obj.isNegative ) {
			range = obj.abs.ceil.asInt.nextPowerOfTwo.max(1) * [-1,1];
		} {
			range = [ 0, obj.ceil.asInt.nextPowerOfTwo.max(1) ];
		};
		
		if( obj.isFloat ) {
			^this.new( range[0], range[1], \lin, 0, obj );
		} {
			^this.new( range[0], range[1], \lin, 1, obj );
		};	
	}
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