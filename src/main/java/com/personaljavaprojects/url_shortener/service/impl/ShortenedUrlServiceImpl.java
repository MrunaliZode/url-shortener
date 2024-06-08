package com.personaljavaprojects.url_shortener.service.impl;

import com.personaljavaprojects.url_shortener.entity.UrlMapping;
import com.personaljavaprojects.url_shortener.exceptions.OriginalUrlNotFoundException;
import com.personaljavaprojects.url_shortener.exceptions.UrlHashingException;
import com.personaljavaprojects.url_shortener.repository.UrlMappingJpaRepository;
import com.personaljavaprojects.url_shortener.service.ShortenedUrlService;
import com.personaljavaprojects.url_shortener.utils.Constants;
import com.personaljavaprojects.url_shortener.utils.UrlShortenerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "shortenedUrl")
public class ShortenedUrlServiceImpl implements ShortenedUrlService {

    private UrlMappingJpaRepository repository;

    @Autowired
    public ShortenedUrlServiceImpl(
            UrlMappingJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @CachePut(cacheNames = "shortenedUrl")
    public String createShortenedUrl(String originalUrl) throws UrlHashingException {

        // check for the shortened URL in database
        UrlMapping urlMapping = repository.findByOriginalURL(originalUrl);
        String shortenedURL;
        if (urlMapping == null) {
            // generate shortened URL
            String shortenedUrlPath = UrlShortenerHelper.generateHash(originalUrl);
            shortenedURL = new StringBuilder(Constants.PROTOCOL)
                    .append(Constants.DOMAIN)
                    .append(Constants.PATH)
                    .append(shortenedUrlPath)
                    .toString();

            // update the database
            UrlMapping databaseObject = new UrlMapping();
            databaseObject.setOriginalURL(originalUrl);
            databaseObject.setShortenedURL(shortenedURL);
            repository.save(databaseObject);
        } else {
            shortenedURL = urlMapping.getShortenedURL();
        }
        return shortenedURL;

    }

    @Override
    @Cacheable(value = "shortenedUrl")
    public String getOriginalUrl(String shortenedUrl) throws OriginalUrlNotFoundException {

        UrlMapping urlMapping = this.repository.findByShortenedURL(shortenedUrl);

        if(urlMapping == null) {
            throw new OriginalUrlNotFoundException(
                    "The mapping of original URL does not exists for the shortened URL");
        }
        return urlMapping.getOriginalURL();
    }
}
