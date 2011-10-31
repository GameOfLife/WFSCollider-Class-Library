/*
    Unit Library
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

    GameOfLife Unit Library: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife Unit Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife Unit Library.  If not, see <http://www.gnu.org/licenses/>.
*/

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
		//^RangeSpec( 0, (minmax[1] * 10).round(1), 0, inf, \lin, 1, minmax.round(1) );
		^PositiveIntegerSpec()
	}
	
	massEditValue { |inArray|
		if( inArray.notNil ) {
			^inArray.minItem;
		} {
			^0;
		};
	}

	massEdit { |inArray, params|
		^(inArray - inArray.minItem) + params;
	}
}