(
~points = all {: Point(6*x,6*y),  x <- [-1,1], y <- [-1,1]};
UnitRack(\wfsSimpleReverb, [\wfsPoint]++
4.collect{|i|[
	U(\simpleReverb)
		.setAudioOut(0,i+1)
		.set(\amp,0.8),
	U(\wfsStaticPlane)
		.setAudioIn(0,i+1)
		.point_(~points[i])
]
}.flop.flat++[U(\wfsDynamicPoint).pointFromBus_(true)]
).category_( \wfs)
)