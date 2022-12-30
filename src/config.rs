use serde::Deserialize;

use figment::{
    providers::{Format, Toml},
    Figment,
};

#[derive(Debug, Deserialize)]
pub struct ApiKeys {
    pub saucenao: String,
}

#[derive(Debug, Deserialize)]
pub struct Database {
    pub url: String
}

#[derive(Debug, Deserialize)]
pub struct AppConfig {
    pub token: String,
    pub apis: ApiKeys,
    pub database: Database,
}

impl AppConfig {
    pub fn figment() -> AppConfig {
        Figment::new()
            .merge(Toml::file("./config.toml"))
            .extract()
            .unwrap()
    }
}
