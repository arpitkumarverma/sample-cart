package com.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.springboot.controller.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CartOfferApplicationTests {

    @Autowired
    private AutowiredController  autowiredController;

    private WireMockServer wireMockServer;

    @Before
    public void setup() {

        wireMockServer = new WireMockServer(options().port(9099));
        wireMockServer.start();

        WireMock.configureFor("localhost", 9099);
    }

    @After
    public void teardown() {
        wireMockServer.stop();
    }

    // valid offer present for the usersegment
    @Test
    public void testValidOffer() throws Exception {
        mockUserSegment(1, "p1");

        boolean isOfferAdded = addOffer(new OfferRequest(1, "FLATX", 10, Arrays.asList("p1")));
        Assert.assertTrue(isOfferAdded);

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(190, val);
    }

    //invalid offerType eg offer_type is other than FLATX or FLATX%

    @Test
    public void testInValidOfferType() throws Exception {
        mockUserSegment(1, "p1");

        boolean isOfferAdded = addOffer(new OfferRequest(1, "%", 10, Arrays.asList("p1")));
        Assert.assertFalse(isOfferAdded);

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(200, val);
    }


    //segment is other than p1, p2, p3
    @Test
    public void testInvalidSegmentType() throws Exception {
        mockUserSegment(1, "p4");

        boolean isOfferAdded = addOffer(new OfferRequest(1, "FLATX", 10, Arrays.asList("p4")));
        Assert.assertTrue(isOfferAdded);

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(200, val);
    }



    @Test
    public void testSegmentMismatch() throws Exception {
        mockUserSegment(2, "p2");

        boolean isOfferAdded =  addOffer(new OfferRequest(1, "FLATX", 10, Arrays.asList("p1")));
        Assert.assertTrue(isOfferAdded);

        int val = applyOffer(new ApplyOfferRequest(200, 2, 1));
        Assert.assertEquals(200, val);
    }



    @Test
    public void testNoOffer() throws Exception {
        mockUserSegment(1, "p1");

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(200, val);
    }



    @Test
    public void testOfferGreaterThanCart() throws Exception {
        mockUserSegment(1, "p1");

        boolean isOfferAdded = addOffer(new OfferRequest(1, "FLATX", 300, Arrays.asList("p1")));
        Assert.assertTrue(isOfferAdded);

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(0, val);
    }



    @Test
    public void testMultipleOffersFirstWins() throws Exception {
        mockUserSegment(1, "p1");

        boolean isOfferAdded = addOffer(new OfferRequest(1, "FLATX", 30, Arrays.asList("p1")));
        Assert.assertTrue(isOfferAdded);
        isOfferAdded = addOffer(new OfferRequest(1, "FLATX", 10, Arrays.asList("p1")));
        Assert.assertTrue(isOfferAdded);

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(170, val);
    }



    @Test
    public void testUserNoSegment() throws Exception {
        mockUserSegment(1, null); // no segment returned

        boolean isOfferAdded = addOffer(new OfferRequest(1, "FLATX", 10, Arrays.asList("p1")));
        Assert.assertTrue(isOfferAdded);

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(200, val);
    }




    @Test
    public void testWrongRestaurant() throws Exception {
        mockUserSegment(1, "p1");

        addOffer(new OfferRequest(2, "FLATX", 10, Arrays.asList("p1"))); // R2, not R1

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(200, val);
    }



    @Test
    public void testUserMultiSegment() throws Exception {
        mockUserSegment(1, "p3");
        mockUserSegment(1, "p1");

        addOffer(new OfferRequest(1, "FLATX", 20, Arrays.asList("p2")));
        addOffer(new OfferRequest(1, "FLATX", 10, Arrays.asList("p1")));

        int val = applyOffer(new ApplyOfferRequest(200, 1, 1));
        Assert.assertEquals(180, val);
    }


	public boolean addOffer(OfferRequest offerRequest) throws Exception {
		String urlString = "http://localhost:9001/api/v1/offer";
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/json");

		ObjectMapper mapper = new ObjectMapper();

		String POST_PARAMS = mapper.writeValueAsString(offerRequest);
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("POST request did not work.");
		}
		return true;
	}


    public int applyOffer(ApplyOfferRequest applyOfferRequest) throws Exception {

        URL url = new URL("http://localhost:9001/api/v1/cart/apply_offer");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(applyOfferRequest);
        OutputStream os = con.getOutputStream();
        os.write(json.getBytes());
        os.close();

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        String rawJson = sb.toString();
        System.out.println("RAW JSON FROM SERVER = " + rawJson);

        ApplyOfferResponse resp = mapper.readValue(rawJson, ApplyOfferResponse.class);

        return resp.getCart_value();
    }


    public void mockUserSegment(int userId, String segment) {
        String responseJson = "{ \"segment\": \"" + segment + "\" }";

        stubFor(
                get(urlPathEqualTo("/api/v1/user_segment"))
                        .withQueryParam("user_id", equalTo(String.valueOf(userId)))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(200)
                                .withBody(responseJson))
        );
    }

}
