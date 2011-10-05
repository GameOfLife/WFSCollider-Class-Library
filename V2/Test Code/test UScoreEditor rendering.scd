
(
r = UChain(0,0,10,false,\sine, \output).fadeOut_(1).fadeIn_(1);
z = UScore(r);
if( z.finiteDuration != 10) { "finiteDuration should be 10".throw };
x = UScoreEditor(z);
y = UScoreEditorGUI(x);

)


(
r = UChain(0,0,10,false,\sine, \output).fadeOut_(1).fadeIn_(1);
v = UChain(0,1,inf,false,\sine, \output).fadeOut_(1).fadeIn_(1);
z = UScore(r,v);
if( z.finiteDuration != 10) { "finiteDuration should be 10".throw };
x = UScoreEditor(z);
y = UScoreEditorGUI(x);

)

z.finiteDuration