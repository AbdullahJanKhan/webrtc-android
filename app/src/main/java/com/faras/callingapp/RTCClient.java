package com.faras.callingapp;

import android.app.Application;
import android.util.Log;

import com.faras.callingapp.models.MessageModels;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RTCClient {

    private Application application;
    private String username;
    private SocketRepository socketRepository;
    private EglBase eglContext;
    private PeerConnectionFactory peerConnectionFactory;
    private List<PeerConnection.IceServer> iceServer;
    private PeerConnection peerConnection;
    private VideoSource localVideoSource;
    private AudioSource localAudioSource;

    private CameraVideoCapturer videoCapturer = null;
    private AudioTrack localAudioTrack = null;
    private VideoTrack localVideoTrack = null;


    public RTCClient(Application application, String username, SocketRepository socketRepository, PeerConnection.Observer observer) {
        this.application = application;
        this.username = username;
        this.socketRepository = socketRepository;
        initPeerConnectionFactory(application);
        this.eglContext = EglBase.create();
        this.peerConnectionFactory = createPeerConnectionFactory();
        try {
            this.iceServer = new ArrayList<>();
            this.iceServer.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
            this.iceServer.add(new PeerConnection.IceServer("stun:openrelay.metered.ca:80"));
            this.iceServer.add(new PeerConnection.IceServer("turn:openrelay.metered.ca:80", "openrelayproject", "openrelayproject"));
            this.iceServer.add(new PeerConnection.IceServer("turn:openrelay.metered.ca:443", "openrelayproject", "openrelayproject"));
            this.iceServer.add(new PeerConnection.IceServer("turn:openrelay.metered.ca:443?transport=tcp", "openrelayproject", "openrelayproject"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.peerConnection = createPeerConnection(observer);
        this.localVideoSource = this.peerConnectionFactory.createVideoSource(false);
        this.localAudioSource = this.peerConnectionFactory.createAudioSource(new MediaConstraints());
    }

    public void initPeerConnectionFactory(Application application) {
        PeerConnectionFactory.InitializationOptions peerConnectionOptions = PeerConnectionFactory
                .InitializationOptions
                .builder(application)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();

        PeerConnectionFactory.initialize(peerConnectionOptions);
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        PeerConnectionFactory.Options peerConnectionFactoryOptions = new PeerConnectionFactory.Options();
        peerConnectionFactoryOptions.disableEncryption = true;
        peerConnectionFactoryOptions.disableNetworkMonitor = true;
        return PeerConnectionFactory
                .builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglContext.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglContext.getEglBaseContext()))
                .setOptions(peerConnectionFactoryOptions)
                .createPeerConnectionFactory();
    }

    private PeerConnection createPeerConnection(PeerConnection.Observer observer) {
        return createPeerConnectionFactory().createPeerConnection(iceServer, observer);
    }

    public void initializeSurfaceView(SurfaceViewRenderer surfaceViewRenderer) {
        surfaceViewRenderer.setEnableHardwareScaler(true);
        surfaceViewRenderer.setMirror(true);
        surfaceViewRenderer.init(eglContext.getEglBaseContext(), null);
    }

    public void startLocalVideo(SurfaceViewRenderer surfaceViewRenderer) {
        this.localVideoSource = this.createPeerConnectionFactory().createVideoSource(false);
        this.localAudioSource = this.createPeerConnectionFactory().createAudioSource(new MediaConstraints());
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), eglContext.getEglBaseContext());
        videoCapturer = getVideoCapture(application);
        videoCapturer.initialize(
                surfaceTextureHelper,
                surfaceViewRenderer.getContext(),
                localVideoSource.getCapturerObserver()
        );
        videoCapturer.startCapture(320, 240, 30);
        localVideoTrack = createPeerConnectionFactory().createVideoTrack("local_track", localVideoSource);
        localVideoTrack.addSink(surfaceViewRenderer);
        localAudioTrack = createPeerConnectionFactory().createAudioTrack("local_track_audio", localAudioSource);
        MediaStream localMediaStream = createPeerConnectionFactory().createLocalMediaStream("local_stream");
        localMediaStream.addTrack(localAudioTrack);
        localMediaStream.addTrack(localVideoTrack);

        peerConnection.addStream(localMediaStream);
    }

    private CameraVideoCapturer getVideoCapture(Application application) {
        Camera2Enumerator enumerator = new Camera2Enumerator(application);
        String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null);
            }
        }

        throw new IllegalStateException();
    }

    public void call(String target) {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                    }

                    @Override
                    public void onSetSuccess() {
                        HashMap<String, Object> offer = new HashMap<>();
                        offer.put("sdp", sessionDescription.description);
                        offer.put("type", sessionDescription.type);

                        socketRepository.sendMessageToSocket(new MessageModels("create_offer", username, target, offer));
                    }

                    @Override
                    public void onCreateFailure(String s) {
                    }

                    @Override
                    public void onSetFailure(String s) {
                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, mediaConstraints);
    }

    public void onRemoteSessionReceived(SessionDescription session) {
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
            }

            @Override
            public void onSetSuccess() {
                Log.d("TAGRTCCLient", "onRemoteSessionReceived: " + session);
            }

            @Override
            public void onCreateFailure(String s) {
            }

            @Override
            public void onSetFailure(String s) {
            }
        }, session);
    }

    public void answer(String target) {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));


        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                    }

                    @Override
                    public void onSetSuccess() {
                        HashMap<String, Object> answer = new HashMap<>();
                        answer.put("sdp", sessionDescription.description);
                        answer.put("type", sessionDescription.type);

                        socketRepository.sendMessageToSocket(new MessageModels("create_answer", username, target, answer));
                    }

                    @Override
                    public void onCreateFailure(String s) {
                    }

                    @Override
                    public void onSetFailure(String s) {
                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, mediaConstraints);
    }

    public void addIceCandidate(IceCandidate p0) {
        peerConnection.addIceCandidate(p0);
    }

    public void switchCamera() {
        videoCapturer.switchCamera(null);
    }

    public void toggleAudio(Boolean mute) {
        localAudioTrack.setEnabled(mute);
    }

    public void toggleCamera(Boolean cameraPause) {
        localVideoTrack.setEnabled(cameraPause);
    }

    public void endCall() {
        peerConnection.close();
    }
}
