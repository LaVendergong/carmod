package com.carmod.client;

/** Pose stashed on a living render state while that entity is riding a jet. */
public interface JetRiderPose {
	void carmod$setJetPose(boolean inJet, float roll, float pitch);

	boolean carmod$inJet();

	float carmod$jetRoll();

	float carmod$jetPitch();
}
