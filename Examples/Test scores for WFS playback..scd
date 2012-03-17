
Udef(\pulse,{ UOut.ar(0,Pulse.ar(\freq.kr(400))*0.1 ) }).sendSynthDef;

//harmonic
//long
(
f = 60.collect({ |i|
		var evt;
		var start = rrand(0.0,10.0);
		var dur = rrand(0.1,3.0);
		evt = UChain(start,i+1,dur,false,\pulse, \output).fadeOut_(0.1*dur).fadeIn_(0.1*dur);
		evt.units[0].set(\freq,100*rrand(1,10).postln );
		evt.units[1].set(\point, rrand(-5.0,5.0)@rrand(-5.0,5.0));
		evt;
	});
x = UScoreEditor(UScore(*f));
y = UScoreEditorGUI(x);

)
//short
(
f = 120.collect({ |i|
		var evt;
		var start = rrand(0.0,10.0);
		var dur = rrand(0.1,0.5);
		evt = UChain(start,0,dur,false,\pulse, \output).fadeOut_(0.1*dur).fadeIn_(0.1*dur);
		evt.units[0].set(\freq, 100*rrand(1,15) );
		evt.units[1].set(\point, rrand(-5.0,5.0)@rrand(-5.0,5.0));
		evt;
	});
x = UScoreEditor(UScore(*f).cleanOverlaps);
y = UScoreEditorGUI(x);

)

//long
(
f = 60.collect({ |i|
		var evt;
		var start = rrand(0.0,10.0);
		var dur = rrand(0.1,3.0);
		evt = UChain(start,0,dur,false,\pulse, \output).fadeOut_(0.1*dur).fadeIn_(0.1*dur);
		evt.units[0].set(\freq, rrand(100.0,600.0) );
		evt.units[1].set(\point, rrand(-5.0,5.0)@rrand(-5.0,5.0));
		evt;
	});
x = UScoreEditor(UScore(*f).cleanOverlaps);
y = UScoreEditorGUI(x);

)
//short
(
f = 120.collect({ |i|
		var evt;
		var start = rrand(0.0,10.0);
		var dur = rrand(0.1,0.5);
		evt = UChain(start,0,dur,false,\pulse, \output).fadeOut_(0.1*dur).fadeIn_(0.1*dur);
		evt.units[0].set(\freq, rrand(100.0,600.0) );
		evt.units[1].set(\point, rrand(-5.0,5.0)@rrand(-5.0,5.0));
		evt;
	});
x = UScoreEditor(UScore(*f).cleanOverlaps);
y = UScoreEditorGUI(x);

)
