VBAPLib {

	*startup {
		var defs;

		Udef.userDefsFolder = File.getcwd +/+ "UnitDefs";

		Udef.defsFolders.add(
            VBAPLib.filenameSymbol.asString.dirname +/+ "UnitDefs"
        );

		UChain.makeDefaultFunc = {
			UChain( \bufSoundFile, \stereoOutput ).useSndFileDur
		};

		if(GUI.scheme == 'qt') {
			UMenuBar();
		};

		defs = Udef.loadAllFromDefaultDirectory.collect(_.synthDef).flat.select(_.notNil)
        ++WFSPrePanSynthDefs.generateAll.flat++WFSPreviewSynthDefs.generateAll;

        Server.default.waitForBoot({

            defs.do({|def|
                def.load( Server.default );
            });
            "\n\tUnit Lib started - VBAP mode".postln
        });

        UGlobalGain.gui;
        UGlobalEQ.gui;
	}

}