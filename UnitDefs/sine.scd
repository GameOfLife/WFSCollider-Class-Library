Udef( \sine, { |freq = 440, amp = 0.7|
	UOut.ar( 0, SinOsc.ar( freq, 0, amp ) )
} )