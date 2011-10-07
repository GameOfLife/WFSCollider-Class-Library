// 1 event with 10 seconds duration
(
r = UChain(0,0,10,false,[\sine,[\freq,rrand(200.0,500.0)]], \output).fadeOut_(1).fadeIn_(1);
z = UScore(r);
if( z.finiteDuration != 10) { "finiteDuration should be 10".throw };
x = UScoreEditor(z);
y = UScoreEditorGUI(x);

)

// 1 event with 10 seconds duration + 1 event with inf duration

(
r = UChain(0,0,10,false,[\sine,[\freq,rrand(200.0,500.0)]], \output).fadeOut_(1).fadeIn_(1);
v = UChain(0,1,inf,false,[\sine,[\freq,rrand(200.0,500.0)]], \output).fadeOut_(1).fadeIn_(1);
z = UScore(r,v);
if( z.finiteDuration != 10) { "finiteDuration should be 10".throw };
x = UScoreEditor(z);
y = UScoreEditorGUI(x);

)

// 10 event with inf seconds duration
(
r = 10.collect{ |i|
	UChain(rrand(0.0,10.0),i,inf,false,[\sine,[\freq,rrand(200.0,500.0)]], \output).fadeOut_(1).fadeIn_(1);
};
z = UScore(*r);
x = UScoreEditor(z);
y = UScoreEditorGUI(x);

)

// score within score within a score 
//double click scores to enter score
(
f = { 12.collect({ |i|
		var evt;
		evt = UChain(i/2,i+1,rrand(3.0,10.0),false,\sine, \output).fadeOut_(1).fadeIn_(1);
		evt.units[0].set(\freq,rrand(200.0,600.0) );
		evt;
	}) };
z = UScore(*f.()++[
	UScore(*f.()++[
		UScore(*f.())
	]
)]);
x = UScoreEditor(z);
y = UScoreEditorGUI(x);

)

Document.openStartup