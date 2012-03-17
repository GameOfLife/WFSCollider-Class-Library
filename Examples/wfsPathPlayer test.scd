
WFSSpeakerConf.default = WFSSpeakerConf.rect( 40, 56, 6, 4.5 );

WFSSpeakerConf.addServer(s,0);

c = UChain( \sine, \wfsPathPlayer, [\wfsDynamicPoint, [ \pointFromBus, true ]] );

c.gui;

