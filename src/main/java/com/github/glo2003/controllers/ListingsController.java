package com.github.glo2003.controllers;

import com.github.glo2003.daos.ListingsDAO;
import com.github.glo2003.daos.MorphiaListingsDAO;
import com.github.glo2003.dtos.ListingDTO;
import com.github.glo2003.dtos.ListingDTOList;
import com.github.glo2003.exceptions.ParameterParsingException;
import com.github.glo2003.helpers.ResponseHelper;
import com.github.glo2003.models.Bookings;
import com.github.glo2003.models.Listing;
import spark.Request;
import spark.Response;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static spark.Spark.*;

public class ListingsController implements Controller{

    static ListingsDAO listingsDAO;

    public ListingsController(ListingsDAO listingsDAO) {
        setupRoutes();
        ListingsController.listingsDAO = listingsDAO;
    }

    public void setupRoutes() {
        path("/listings", () -> {
            get("", this::getAllListings, ResponseHelper::serializeObjectToJson);
            post("", this::addListing, ResponseHelper::serializeObjectToJson);
            path("/:id", () -> {
                before("", this::validateListing);
                before("/*", this::validateListing);
                get("", this::getListing, ResponseHelper::serializeObjectToJson);
                post("/book", this::bookListing, ResponseHelper::serializeObjectToJson);
                get("/rate/:score", this::addRating, ResponseHelper::serializeObjectToJson);
            });
        });
    }

    private LocalDate parseDateFromParam(String stringDate) throws ParameterParsingException {
        try {
            return LocalDate.parse(stringDate);
        } catch (DateTimeParseException e) {
            throw new ParameterParsingException("date", "LocalDate");
        }
    }

    private void validateListing(Request req, Response res) throws Exception {
        String id = req.params("id");
        Listing listing = listingsDAO.get(id);
        req.attribute("listing", listing);
    }

    private Object getListing(Request req, Response res) throws Exception {
        Listing listing = req.attribute("listing");
        return new ListingDTO(listing);
    }

    private Object getAllListings(Request req, Response res) throws Exception{
        String dateString = req.queryParams("date");
        String titleString = req.queryParams("title");
        if(dateString != null){
            LocalDate date = parseDateFromParam(dateString);
            return new ListingDTOList(listingsDAO.getAllSpecificDate(date));
        } else if(titleString != null) {
            return new ListingDTOList(listingsDAO.getAllWithTitle(titleString));
        } else {
            return new ListingDTOList(listingsDAO.getAll());
        }
    }

    private Object addListing(Request req, Response res) throws Exception {
        Listing listing = ResponseHelper.deserializeJsonToObject(req.body(), Listing.class);
        String id = listingsDAO.save(listing);
        res.header("Location", String.format("/listings/%s", id));
        res.status(201);
        return ResponseHelper.EMPTY_RESPONSE;
    }

    private Object bookListing(Request req, Response res) throws Exception {
        Listing listing = req.attribute("listing");
        Bookings bookings = ResponseHelper.deserializeJsonToObject(req.body(), Bookings.class);
        listing.book(bookings.getBookings());

        res.status(204);
        return ResponseHelper.EMPTY_RESPONSE;
    }

    private Object addRating(Request req, Response res) throws Exception {
        String listingId = req.params("id");
        Integer rating = Integer.parseInt(req.params("score"));
        Listing listing = listingsDAO.get(listingId);
        listing.addRating(rating);

        if(MorphiaListingsDAO.class.isInstance(listingsDAO)){
            listingsDAO.save(listing);
        }
        res.status(204);
        return ResponseHelper.EMPTY_RESPONSE;
    }

}