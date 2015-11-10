ArrayGeneratorDef( \pulse, { |f, n = 1|
	var phase, width, periods, range, close;
	periods = f.get( \periods );	
	width = f.get( \width ).linlin(0,1,1,-1);
	phase = f.get( \phase );
	range = f.get( \range );
	close = f.get( \close ).not.binaryValue;
	n.collect({ |i|
		((i.linlin(0,n-close,0,2 * periods) + phase + width).wrap(-1,1) > width)
			.binaryValue.linlin(0,1,*range);
	});
	
}, [ \range, [0.0,1.0], \periods, 1, \width, 0.5, \phase, 0, \close, true ], { |f, obj|
	[ 
		\range, [ obj.minItem, obj.maxItem ], 
		\periods, f.get( \periods ), 
		\width, f.get( \width ), 
		\phase, f.get( \phase ),
		\close, f.get( \close ) 
	]
} )
	.setSpec( \periods, [0, inf, \lin, 0.125].asSpec )
	.setSpec( \width, [0, 1, \lin, 0, 0.5].asSpec )
	.setSpec( \phase, [0,2,\lin].asSpec )
	.setSpec( \range, [0,1].asSpec.asRangeSpec );