package com.example.diplom.conf;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setCollectionsMergeEnabled(false)
                .setAmbiguityIgnored(true);

        mapper.addConverter(new Converter<Collection<?>, List<?>>() {
            @Override
            public List<?> convert(MappingContext<Collection<?>, List<?>> context) {
                Collection<?> source = context.getSource();
                return (source == null)
                        ? null
                        : new ArrayList<>(source);
            }
        });
        return mapper;
    }
}
