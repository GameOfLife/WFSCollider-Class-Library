Udef( \whiteNoise, { |amp = 0.1|
	UOut.ar( 0, WhiteNoise.ar * amp )
} ).category_( \synthesis )