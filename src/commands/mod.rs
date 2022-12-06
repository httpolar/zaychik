pub mod avatar;
pub mod ping;


pub async fn get_commands() -> Vec<poise::Command<crate::Data, crate::Error>> {
    vec![self::avatar::avatar(), self::ping::ping()]
}
