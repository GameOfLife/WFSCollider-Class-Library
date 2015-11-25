ArrayGeneratorDef( \linrand, { |f, n = 1|
	var range;
	range = f.get( \range );
	thisThread.randSeed = f.get( \seed );
	n.collect({ |i|
		1.0.linrand.linlin(0,1,*range);
	});
	
}, [ \range, [0.0,1.0], \seed, 12345 ], { |f, obj|
	[ \range, [ obj.minItem, obj.maxItem ], \seed, 1569325056.rand ]
} )
	.setSpec( \range, [0,1].asSpec.asRangeSpec )
	.setSpec( \seed, PositiveIntegerSpec(12345) );