use serde::Deserialize;

use figment::{
    providers::{Format, Toml},
    Figment,
};

#[derive(Debug, Deserialize)]
pub struct AppConfig {
    pub token: String,
}

impl AppConfig {
    pub fn figment() -> AppConfig {
        Figment::new()
            .merge(Toml::file("./config.toml"))
            .extract()
            .unwrap()
    }
}
