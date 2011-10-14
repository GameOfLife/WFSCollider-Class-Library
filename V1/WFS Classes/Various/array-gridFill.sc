/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

//// v1.0ws  -- addition to Raviv Space.sc
//// extra methods for Array

+ Symbol { 
	detectLevels { |inLevel = 0 | ^inLevel }
	}

+ String { 
	detectLevels { |inLevel = 0 | ^inLevel }
	}
	
+ Object { 
	detectLevels { |inLevel = 0 | ^inLevel }
	}
	
+ Array {
	addToAll{
		arg add, reverseOdd = true;
		var arrayOfArrays, outArray;
		arrayOfArrays = this ? [ 0, 1.53, 3.06, 4.59 ];
		add = add ? [ 0, 1.8, 3.6 ];
		if (add.isArray.not) { add = [add] };
		outArray = [];
		
		add.do({ |addItem, addI|
			var localArray;
			localArray = arrayOfArrays.collect({ |arrayItem, arrayI|
					if (arrayItem.isArray.not) { arrayItem = [arrayItem] };
					arrayItem ++ addItem });				if (addI.odd && reverseOdd) { localArray = localArray.reverse };
		outArray = outArray ++ localArray
		});
		^outArray;
	}
	
	detectLevels {arg inLevel = 0; //hierarchical level detection
		if(this.containsSeqColl)
				{	^this[0].detectLevels( inLevel + 1) }
				{	^inLevel }
		}
	
	*gridFill{ arg inArray, reverseOdd = true;
		var outArray;
		inArray = inArray ? [[10, 20, 30], [1,2,3]];
		outArray = inArray.at(0);
		(inArray.size - 1).do({
			|i| outArray = outArray.addToAll(inArray.at(i + 1), reverseOdd) });
		^outArray;

	}
	*makeGrid{ arg width, div, offset, reverseOdd = true;
		var size, axisPoints, outArray;
		axisPoints = [];
		width = width ? [4.59, 3.6, 0];
		div = div ? [4,3,1];
		size = 1;
		div.do({ |item| size = size * item; });
		offset =  offset ? width.collect({ 0 });
		if (width.size != div.size) { "width and div sizes are not equal!".warn };
		div.do({ |item, i| 
			axisPoints = axisPoints.add(
				Array.fill(item, { |ii| 
					if(item == 1) { width.at(i) * 1.5 } 
					{ (ii / (item - 1).max(0.000001)) * width.at(i);  };
				}) );
		});
		axisPoints = (axisPoints - (width / 2)) + offset;
		^Array.gridFill(axisPoints, reverseOdd);
	}	
	
	*makeLine{ arg start, end, div, includeEnd = true;
		start = start ? [-1, 0, 0];
		end = end ? [1, 0, 0];
		div = div ? 9;
		^Array.fill(div - (if (includeEnd) {0} {1}), { arg i;
			Array.fill(start.size, { arg ii;
				((i / (div - 1)) * (end.at(ii) - start.at(ii))) + start.at(ii)
			})
		});
	}
	*makeRect{
		arg width, div, offset; // div per side, offset includes z
		var outArray, cornerPoints;
		width = width ? [2,3];
		div = div ? 5;
		offset = offset ? [0,0,0];
		outArray = Array.new;
		cornerPoints = [	[width.at(0) / -2, width.at(1) / 2, 0],
					[width.at(0) / 2, width.at(1) / 2, 0],
					[width.at(0) / 2, width.at(1) / -2, 0],
					[width.at(0) / -2, width.at(1) / -2, 0] ]
			+.t offset;
				
		4.do({ arg i;
			outArray = outArray ++ Array.makeLine(
				cornerPoints.at(i), cornerPoints.wrapAt(i+1), div, false)
		});
		^outArray;
	}
	
}