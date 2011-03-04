WFSSplinePath : WFSPath2 {
	
	var <c1, <c2;
	
	init {
		times = times ?? {[1]};
		c1 = Order(); // user defined control points are stored here
		c2 = Order();
	}
	
	controls {
		var autoControls;
		if( (c1.size < positions.size) or: { c2.size < positions.size } )
			{ autoControls = this.generateAutoControls };
			
		^positions.collect({ |item, i|
				[ c1[i] ?? { autoControls[i][0] },
				  c2[i] ?? { autoControls[i][1] } ];
			});
	}
	
	fillControls {
		var autoControls;
		// fill controls with current intType etc. setting
		autoControls = this.generateAutoControls;
		#c1, c2 = autoControls.flop.collect(_.as(Order));
	}
	
	putC1 { |index, inC1, absolute = false| // index and inC1 can be arrays
		this.prPutC( index, inC1, absolute, c1 );
	}
	
	putC2 { |index, inC2, absolute = false|
		this.prPutC( index, inC2, absolute, c2 );
	}
	
	prPutC { |index, inC, absolute = false, c|
		var array;
		c = c ? c1; 
		inC = inC.asCollection; 
		index = index.asCollection;
		array = index.collect({ |item, i| [ item, 
					inC.wrapAt( i ) !? { inC.wrapAt( i ).asPoint } ]; }); 		array.do({ |item| c[ item[0] ] = item[1]; });
	}
	
	clearControls { [ c1, c2 ].do(_.makeEmpty) }
	
	trimControls { [ c1, c2 ].do({ |order|
				order = order.select({ |item, i| i < positions.size });
			});
	}
	
}