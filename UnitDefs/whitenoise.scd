Udef( \whitenoise, { |freq = 440, amp = 0.1|
	UOut.ar( 0, WhiteNoise.ar * amp )
} )