+ Spec {
	
	massEditSpec { ^nil }
	
	massEditValue { ^0 }
	
	massEdit { |inArray, params|
		^inArray;  // no mass editing for default spec
	}
	
}

+ Nil {
	massEditSpec { ^nil }
}

+ ControlSpec {
	
	massEditSpec { |inArray|
		var minmax;
		minmax = this.massEditValue( inArray );
		^RangeSpec( minval, maxval, 1.0e-11, inf, warp, step, minmax, units );
	}
	
	massEditValue { |inArray|
		if( inArray.notNil ) {
			^[ inArray.minItem, inArray.maxItem ];
		} {
			^[minval, maxval];
		};
	}
	
	massEdit { |inArray, params|
		var linlinArgs;
		linlinArgs = this.unmap( this.massEditValue( inArray ) ++ params );
		^inArray.collect({ |item|
			this.map( this.unmap( item ).linlin( *linlinArgs ) );
		});
	}
	
}

+ RangeSpec {
	
	massEditSpec { |inArray|
		var minmax;
		minmax = this.massEditValue( inArray );
		^RangeSpec( minval, maxval, 1.0e-11, inf, warp, step, minmax, units );
	}
	
	massEditValue { |inArray|
		if( inArray.notNil ) {
			^[ inArray.flat.minItem, inArray.flat.maxItem ];
		} {
			^[minval, maxval];
		};
	}
}

+ PositiveIntegerSpec {
	
	massEditSpec { |inArray|
		var minmax;
		minmax = this.massEditValue( inArray );
		^RangeSpec( 0, (minmax[1] * 10).round(1), 0, inf, \lin, 1, minmax.round(1) );
	}
	
	massEditValue { |inArray|
		if( inArray.notNil ) {
			^[ inArray.minItem, inArray.maxItem ];
		} {
			^[0, inf];
		};
	}

	massEdit { |inArray, params|
		var linlinArgs;
		linlinArgs = this.massEditValue( inArray ) ++ params;
		^inArray.collect({ |item|
			this.constrain( item.linlin( *linlinArgs ) );
		});
	}
}