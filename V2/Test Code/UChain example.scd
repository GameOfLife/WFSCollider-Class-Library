(
// create some Udefs

Udef( \sine, { |freq = 440, amp = 0.1|
	UOut.ar( 0, SinOsc.ar( freq, 0, amp ) )
} ).loadSynthDef;

Udef( \mix2, { |balance = 0.5|
	var l1, l2;
	l1 = UIn.ar( 0 ) * (1-balance);
	l2 = UIn.ar( 1 ) * balance;
	UOut.ar( 0, l1 + l2 )
} ).loadSynthDef;

Udef( \vibrato, { |rate = 1, amount = #[0.0,1.0]|
	UOut.ar( 0, SinOsc.kr( rate ).range(*amount) * UIn.ar( 0 ) )
} ).loadSynthDef;

Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) * UEnv.kr ); // UEnv has the power to release the chain 
} ).setSpec( \bus, [0,7,\lin,1] ).loadSynthDef;

)

x = UChain( \sine, [ \sine, [ \freq, 550, \u_o_ar_0_bus, 1 ]], \mix2, \vibrato, \output );

x.start;
x.release;

x.setFadeOut( 0.5 );
x.setFadeIn( 0.5 );

x.setDur( 5 );
x.setDur( inf ); // infinite duration (default)

x[1].setAudioOut( 0, 1 ); // 2nd sine output to bus 1, so it goes to second mix input

x.gui; // gui without styling

( // gui with styling
w = Window( "x", Rect( 300,25,200,300 ) ).front;
RoundView.useWithSkin( ( 
	labelWidth: 40, 
	font: Font( Font.defaultSansFace, 10 ), 
	hiliteColor: Color.gray(0.33)
), { x.gui(w); };
);
)